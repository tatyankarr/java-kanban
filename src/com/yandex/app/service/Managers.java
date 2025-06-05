package com.yandex.app.service;

import com.yandex.app.interfaces.HistoryManager;
import com.yandex.app.interfaces.TaskManager;

public class Managers {

    private Managers() {};

    public static TaskManager getDefault() {
        return new InMemoryTaskManager(getDefaultHistory());
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
