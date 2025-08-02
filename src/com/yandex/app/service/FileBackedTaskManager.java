package com.yandex.app.service;

import com.yandex.app.model.*;
import com.yandex.app.enums.*;
import com.yandex.app.exceptions.ManagerSaveException;
import com.yandex.app.interfaces.HistoryManager;

import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file, HistoryManager historyManager) {
        super(historyManager);
        this.file = file;
    }

    protected void save() {
        try (Writer writer = new FileWriter(file)) {
            writer.write("id,type,name,status,description,epic,duration,startTime\n");
            for (Task task : getAllTasks()) {
                writer.write(taskToString(task) + "\n");
            }
            for (Epic epic : getAllEpics()) {
                writer.write(taskToString(epic) + "\n");
            }
            for (Subtask subtask : getAllSubtasks()) {
                writer.write(taskToString(subtask) + "\n");
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при сохранении данных в файл", e);
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        HistoryManager historyManager = Managers.getDefaultHistory();
        FileBackedTaskManager manager = new FileBackedTaskManager(file, historyManager);
        Map<Integer, Epic> tempEpics = new HashMap<>();

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            int maxId = 0;

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;

                Task task = taskFromString(line);
                maxId = Math.max(maxId, task.getId());

                if (task instanceof Epic) {
                    tempEpics.put(task.getId(), (Epic) task);
                    manager.getEpicsMap().put(task.getId(), (Epic) task);
                } else if (task instanceof Subtask) {
                    manager.getSubtasksMap().put(task.getId(), (Subtask) task);
                } else {
                    manager.getTasksMap().put(task.getId(), task);
                }
            }

            manager.setIdCounter(maxId + 1);

            for (Subtask subtask : manager.getSubtasksMap().values()) {
                Epic epic = tempEpics.get(subtask.getEpicId());
                if (epic != null) {
                    epic.addSubtask(subtask.getId());
                }
            }

            for (Epic epic : manager.getAllEpics()) {
                epic.updateTimeFields(manager.getSubtasksMap());
                manager.updateEpicStatus(epic.getId());
            }

        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при загрузке данных из файла", e);
        }

        for (Epic epic : manager.getAllEpics()) {
            epic.updateTimeFields(manager.getSubtasksMap());
        }

        return manager;
    }

    private void restoreTask(Task task) {
        int id = task.getId();

        if (getTask(id) != null || getEpic(id) != null || getSubtask(id) != null) {
            return;
        }

        if (task instanceof Subtask) {
            getSubtasksMap().put(id, (Subtask) task);
            Epic epic = getEpicsMap().get(((Subtask) task).getEpicId());
            if (epic != null) {
                epic.addSubtask(id);
            }
        } else if (task instanceof Epic) {
            getEpicsMap().put(id, (Epic) task);
        } else {
            getTasksMap().put(id, task);
        }

        if (id >= getIdCounter()) {
            setIdCounter(id + 1);
        }
    }

    protected Map<Integer, Subtask> getSubtasksMap() {
        return super.getSubtasksMap();
    }

    @Override
    public void createTask(Task task) {
        super.createTask(task);
        save();
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void removeTask(int id) {
        super.removeTask(id);
        save();
    }

    @Override
    public void createEpic(Epic epic) {
        super.createEpic(epic);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void removeEpic(int id) {
        super.removeEpic(id);
        save();
    }

    @Override
    public void createSubtask(Subtask subtask) {
        super.createSubtask(subtask);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void removeSubtask(int id) {
        super.removeSubtask(id);
        save();
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }
        return value;
    }

    private static String taskToString(Task task) {
        String type = (task instanceof Epic) ? "EPIC" :
                (task instanceof Subtask) ? "SUBTASK" : "TASK";

        String epicId = (task instanceof Subtask) ? String.valueOf(((Subtask) task).getEpicId()) : "";
        String name = escapeCsv(task.getName());
        String description = escapeCsv(task.getDescription());
        String status = (task.getStatus() != null) ? task.getStatus().toString() : "NEW";
        String durationStr = task.getDuration() != null ? String.valueOf(task.getDuration().toMinutes()) : "";
        String startTimeStr = task.getStartTime() != null ? task.getStartTime().toString() : "";

        return String.join(",",
                String.valueOf(task.getId()),
                type,
                name,
                status,
                description,
                epicId,
                durationStr,
                startTimeStr
        );
    }

    private static Task taskFromString(String value) {
        List<String> fields = parseCsvLine(value);

        if (fields.size() < 8) {
            throw new ManagerSaveException(
                    "Ошибка разбора строки задачи: ожидалось 8 полей, но получено " +
                            fields.size() + ". Строка: " + value
            );
        }

        int id = Integer.parseInt(fields.get(0));
        TaskType type = TaskType.valueOf(fields.get(1));
        String name = fields.get(2);
        Status status = Status.valueOf(fields.get(3));
        String description = fields.get(4);
        long durationMinutes = fields.get(6).isEmpty() ? 0 : Long.parseLong(fields.get(6));
        Duration duration = Duration.ofMinutes(durationMinutes);
        LocalDateTime startTime = fields.get(7).isEmpty() ? null : LocalDateTime.parse(fields.get(7));

        switch (type) {
            case TASK:
                Task task = new Task(name, description, status);
                task.setId(id);
                task.setDuration(duration);
                task.setStartTime(startTime);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(fields.get(5));
                Subtask subtask = new Subtask(name, description, status, epicId);
                subtask.setId(id);
                subtask.setDuration(duration);
                subtask.setStartTime(startTime);
                return subtask;
            default:
                throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
        }
    }

    private static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());

        while (fields.size() < 8) {
            fields.add("");
        }

        return fields;
    }
}