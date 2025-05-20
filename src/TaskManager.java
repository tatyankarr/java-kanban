import java.util.ArrayList;
import java.util.HashMap;

public class TaskManager {
    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private final HashMap<Integer, Epic> epics = new HashMap<>();
    private final HashMap<Integer, Subtask> subtasks = new HashMap<>();
    private int idForTask = 1;
    private int idForEpic = 1;
    private int idForSubtask = 1;

    private int generateIdTask() {
        return idForTask++;
    }

    private int generateIdEpic() {
        return idForEpic++;
    }

    private int generateIdSubtask() {
        return idForSubtask++;
    }

    private void updateEpicStatus (int epicId) {
        Epic epic = epics.get(epicId);
        ArrayList<Integer> subIds = epic.getSubtaskId();

        if (subIds.isEmpty()) {
            epic.status = Status.NEW;
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (int subId : subIds) {
            Status status = subtasks.get(subId).status;
            if (status != Status.NEW) {
                allNew = false;
            }
            if (status != Status.DONE) {
                allDone = false;
            }
        }

        if (allDone) {
            epic.status = Status.DONE;
        } else if (allNew) {
            epic.status = Status.NEW;
        } else {
            epic.status = Status.IN_PROGRESS;
        }
    }

    public void createTask(Task task) {
        task.id = generateIdTask();
        tasks.put(task.id, task);
    }

    public ArrayList<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    public void clearAllTasks() {
        tasks.clear();
    }

    public Task getTask(int id) {
        return tasks.get(id);
    }

    public void updateTask(Task task) {
        if (tasks.containsKey(task.id)) {
            tasks.put(task.id, task);
        }
    }

    public void removeTask(int id) {
        tasks.remove(id);
    }

    public void createEpic (Epic epic) {
        epic.id = generateIdEpic();
        epics.put(epic.id, epic);
    }

    public ArrayList<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    public void clearAllEpics() {
        for (Epic epic : epics.values()) {
            for (int subId : epic.getSubtaskId()) {
                subtasks.remove(subId);
            }
        }
        epics.clear();
    }

    public Epic getEpic(int id) {
        return epics.get(id);
    }

    public void updateEpic(Epic epic) {
        if (epics.containsKey(epic.id)) {
            Epic oldEpic = epics.get(epic.id);
            epic.getSubtaskId().addAll(oldEpic.getSubtaskId());
            epics.put(epic.id, epic);
            updateEpicStatus(epic.id);
        }
    }

    public void removeEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (int subId : epic.getSubtaskId()) {
                subtasks.remove(subId);
            }
        }
    }

    public void createSubtask(Subtask subtask) {
        if (!epics.containsKey(subtask.getEpicId())) {
            return;
        }
        subtask.id = generateIdSubtask();
        subtasks.put(subtask.id, subtask);
        epics.get(subtask.getEpicId()).addSubtask(subtask.id);
        updateEpicStatus(subtask.getEpicId());
    }

    public ArrayList<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    public void clearAllSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtask();
        }
        subtasks.clear();
    }

    public Subtask getSubtask(int id) {
        return subtasks.get(id);
    }

    public void updateSubtask(Subtask subtask) {
        if (subtasks.containsKey(subtask.id)) {
            subtasks.put(subtask.id, subtask);
            updateEpicStatus(subtask.getEpicId());
        }
    }

    public void removeSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.deleteSubstackId(id);
                updateEpicStatus(epic.id);
            }
        }
    }
}
