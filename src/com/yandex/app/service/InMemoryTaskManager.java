package com.yandex.app.service;

import com.yandex.app.interfaces.HistoryManager;
import com.yandex.app.interfaces.TaskManager;
import com.yandex.app.model.*;
import com.yandex.app.enums.Status;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final HistoryManager historyManager;
    private final Set<Task> prioritizedTasks = new TreeSet<>(Comparator
            .comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::getId));

    private int idCounter = 1;

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    private int generateId() {
        return idCounter++;
    }

    protected void updateEpicStatus(int epicId) {
        Epic epic = epics.get(epicId);
        List<Integer> subIds = epic.getSubtaskId();

        if (subIds.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (int subId : subIds) {
            Status status = subtasks.get(subId).getStatus();
            if (status != Status.NEW) {
                allNew = false;
            }
            if (status != Status.DONE) {
                allDone = false;
            }
            if (!allNew && !allDone) {
                epic.setStatus(Status.IN_PROGRESS);
                return;
            }
        }

        if (allDone) {
            epic.setStatus(Status.DONE);
        } else {
            epic.setStatus(Status.NEW);
        }
    }

    protected Map<Integer, Task> getTasksMap() {
        return tasks;
    }

    protected Map<Integer, Epic> getEpicsMap() {
        return epics;
    }

    protected Map<Integer, Subtask> getSubtasksMap() {
        return subtasks;
    }

    protected void setIdCounter(int value) {
        idCounter = value;
    }

    protected int getIdCounter() {
        return idCounter;
    }

    private boolean hasIntersection(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getEndTime() == null) {
            return false;
        }

        return prioritizedTasks.stream()
                .filter(t -> t.getId() != newTask.getId() && t.getStartTime() != null && t.getEndTime() != null)
                .anyMatch(existing -> newTask.getStartTime().isBefore(existing.getEndTime()) &&
                        existing.getStartTime().isBefore(newTask.getEndTime()));
    }

    public List<Subtask> getEpicSubtasks(int epicId) {
        return subtasks.values().stream()
                .filter(s -> s.getEpicId() == epicId)
                .collect(Collectors.toList());
    }

    @Override
    public void createTask(Task task) {
        if (hasIntersection(task)) {
            throw new IllegalArgumentException("Задача пересекается с другой по времени");
        }
        task.setId(generateId());
        tasks.put(task.getId(), task);
        prioritizedTasks.add(task);
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void clearAllTasks() {
        tasks.clear();
    }

    @Override
    public Task getTask(int id) {
        Task task = tasks.get(id);
        if (task != null) {
            historyManager.add(task);
        }
        return task;
    }

    @Override
    public void updateTask(Task task) {
        if (tasks.containsKey(task.getId())) {
            if (hasIntersection(task)) {
                throw new IllegalArgumentException("Задача пересекается с другой по времени");
            }
            prioritizedTasks.remove(tasks.get(task.getId()));
            tasks.put(task.getId(), task);
            prioritizedTasks.add(task);
        }
    }

    @Override
    public void removeTask(int id) {
        Task task = tasks.remove(id);
        if (task != null) {
            prioritizedTasks.remove(task);
            historyManager.remove(id);
        }
    }

    @Override
    public void createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void clearAllEpics() {
        epics.values().stream()
                .flatMap(epic -> epic.getSubtaskId().stream())
                .forEach(subtasks::remove);
        epics.clear();
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.getId())) {
            Epic oldEpic = epics.get(epic.getId());
            epic.getSubtaskId().addAll(oldEpic.getSubtaskId());
            epics.put(epic.getId(), epic);
            updateEpicStatus(epic.getId());
        }
    }

    @Override
    public void removeEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            epic.getSubtaskId().forEach(subId -> {
                subtasks.remove(subId);
                historyManager.remove(subId);
            });
            historyManager.remove(id);
        }
    }

    @Override
    public void createSubtask(Subtask subtask) {
        if (!epics.containsKey(subtask.getEpicId())) {
            return;
        }
        if (hasIntersection(subtask)) {
            throw new IllegalArgumentException("Подзадача пересекается с другой по времени");
        }
        subtask.setId(generateId());
        subtasks.put(subtask.getId(), subtask);
        prioritizedTasks.add(subtask);
        epics.get(subtask.getEpicId()).addSubtask(subtask.getId());
        updateEpicStatus(subtask.getEpicId());
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public void clearAllSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtask();
        }
        subtasks.clear();
    }

    @Override
    public Subtask getSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtask;
    }

    @Override
    public Subtask getSubtaskWithoutHistory(int id) {
        return subtasks.get(id);
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        if (subtasks.containsKey(subtask.getId())) {
            if (hasIntersection(subtask)) {
                throw new IllegalArgumentException("Подзадача пересекается с другой по времени");
            }
            prioritizedTasks.remove(subtasks.get(subtask.getId()));
            subtasks.put(subtask.getId(), subtask);
            prioritizedTasks.add(subtask);
            updateEpicStatus(subtask.getEpicId());
            epics.get(subtask.getEpicId()).updateTimeFields(subtasks);
        }
    }

    @Override
    public void removeSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.deleteSubstackId(id);
                updateEpicStatus(epic.getId());
                epic.updateTimeFields(subtasks);
            }
            prioritizedTasks.remove(subtask);
            historyManager.remove(id);
        }
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }
}
