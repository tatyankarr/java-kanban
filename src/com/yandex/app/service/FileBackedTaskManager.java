package com.yandex.app.service;

import com.yandex.app.model.*;
import com.yandex.app.enums.*;
import com.yandex.app.exceptions.ManagerSaveException;
import com.yandex.app.interfaces.HistoryManager;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final File file;

    public FileBackedTaskManager(File file, HistoryManager historyManager) {
        super(historyManager);
        this.file = file;
    }

    protected void save() {
        try (Writer writer = new FileWriter(file)) {
            writer.write("id,type,name,status,description,epic\n");
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

        try {
            List<String> lines = Files.readAllLines(file.toPath());

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;

                Task task = taskFromString(line);
                if (task instanceof Epic) {
                    manager.restoreTask(task);
                }
            }

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank()) continue;

                Task task = taskFromString(line);
                if (!(task instanceof Epic)) {
                    manager.restoreTask(task);
                }
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Ошибка при загрузке данных из файла", e);
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

        return String.join(",",
                String.valueOf(task.getId()),
                type,
                name,
                status,
                description,
                epicId
        );
    }

    private static Task taskFromString(String value) {
        List<String> fields = parseCsvLine(value);

        if (fields.size() < 6) {
            throw new ManagerSaveException(
                    "Ошибка разбора строки задачи: ожидалось 6 полей, но получено " +
                            fields.size() + ". Строка: " + value
            );
        }

        int id = Integer.parseInt(fields.get(0));
        TaskType type = TaskType.valueOf(fields.get(1));
        String name = fields.get(2);
        Status status = Status.valueOf(fields.get(3));
        String description = fields.get(4);

        switch (type) {
            case TASK:
                Task task = new Task(name, description, status);
                task.setId(id);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(fields.get(5));
                Subtask subtask = new Subtask(name, description, status, epicId);
                subtask.setId(id);
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
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        sb.append('\"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '\"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString());
        return fields;
    }
}