package com.yandex.app.interfaces;

import com.yandex.app.model.*;

import java.util.ArrayList;
import java.util.List;

public interface TaskManager {
    void createTask(Task task);
    ArrayList<Task> getAllTasks();
    void clearAllTasks();
    Task getTask(int id);
    void updateTask(Task task);
    void removeTask(int id);

    void createEpic(Epic epic);
    ArrayList<Epic> getAllEpics();
    void clearAllEpics();
    Epic getEpic(int id);
    void updateEpic(Epic epic);
    void removeEpic(int id);

    void createSubtask(Subtask subtask);
    ArrayList<Subtask> getAllSubtasks();
    void clearAllSubtasks();
    Subtask getSubtask(int id);
    Subtask getSubtaskWithoutHistory(int id);
    void updateSubtask(Subtask subtask);
    void removeSubtask(int id);

    List<Task> getHistory();
}
