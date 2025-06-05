package com.yandex.app.tests;

import com.yandex.app.enums.Status;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.service.InMemoryTaskManager;
import com.yandex.app.service.InMemoryHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {

    private InMemoryTaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager(new InMemoryHistoryManager());
    }

    @Test
    void shouldAddAndFindTasksOfDifferentTypesById() {
        Task task = new Task("Task 1", "Description 1", Status.NEW);
        taskManager.createTask(task);
        int taskId = task.getId();

        Epic epic = new Epic("Epic 1", "Epic Description");
        taskManager.createEpic(epic);
        int epicId = epic.getId();

        Subtask subtask = new Subtask("Subtask 1", "Subtask Description", Status.NEW, epicId);
        taskManager.createSubtask(subtask);
        int subtaskId = subtask.getId();

        Task foundTask = taskManager.getTask(taskId);
        assertNotNull(foundTask, "Обычная задача должна быть найдена");
        assertEquals(taskId, foundTask.getId());
        assertEquals("Task 1", foundTask.getName());

        Epic foundEpic = taskManager.getEpic(epicId);
        assertNotNull(foundEpic, "Эпик должен быть найден");
        assertEquals(epicId, foundEpic.getId());
        assertEquals("Epic 1", foundEpic.getName());

        Subtask foundSubtask = taskManager.getSubtask(subtaskId);
        assertNotNull(foundSubtask, "Подзадача должна быть найдена");
        assertEquals(subtaskId, foundSubtask.getId());
        assertEquals("Subtask 1", foundSubtask.getName());
        assertEquals(epicId, foundSubtask.getEpicId());
    }
}
