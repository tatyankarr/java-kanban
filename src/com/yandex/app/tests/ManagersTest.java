package com.yandex.app.tests;

import com.yandex.app.enums.Status;
import com.yandex.app.interfaces.HistoryManager;
import com.yandex.app.interfaces.TaskManager;
import com.yandex.app.service.Managers;
import com.yandex.app.model.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManagersTest {

    @Test
    void shouldReturnInitializedTaskManager() {
        TaskManager taskManager = Managers.getDefault();
        assertNotNull(taskManager, "TaskManager должен быть проинициализирован");

        Task task = new Task("Test Task", "Test Description", Status.NEW);
        taskManager.createTask(task);

        List<Task> allTasks = taskManager.getAllTasks();
        assertEquals(1, allTasks.size(), "TaskManager должен содержать одну задачу");
        assertEquals("Test Task", allTasks.get(0).getName(), "Имя задачи должно совпадать");
    }

    @Test
    void shouldReturnInitializedHistoryManager() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        assertNotNull(historyManager, "HistoryManager должен быть проинициализирован");

        Task task = new Task("History Task", "Test History Description", Status.NEW);
        historyManager.add(task);

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История должна содержать одну задачу");
        assertEquals("History Task", history.get(0).getName(), "Имя задачи должно совпадать в истории");
    }

    @Test
    void taskManagerShouldHaveWorkingHistoryManager() {
        TaskManager taskManager = Managers.getDefault();
        Task task = new Task("History Through Manager", "Desc", Status.NEW);
        taskManager.createTask(task);

        List<Task> history = taskManager.getHistory();
        assertEquals(1, history.size(), "История через TaskManager должна содержать одну задачу");
        assertEquals("History Through Manager", history.get(0).getName(), "Имя задачи должно совпадать в истории через TaskManager");
    }
}
