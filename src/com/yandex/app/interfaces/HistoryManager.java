package com.yandex.app.interfaces;

import com.yandex.app.model.Task;

import java.util.List;

public interface HistoryManager {
    void add(Task task);
    List<Task> getHistory();
}
