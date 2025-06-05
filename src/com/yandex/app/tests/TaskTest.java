package com.yandex.app.tests;

import com.yandex.app.service.InMemoryHistoryManager;
import com.yandex.app.service.InMemoryTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.yandex.app.enums.Status;
import com.yandex.app.model.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    private InMemoryTaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager(new InMemoryHistoryManager());
    }

    @Test
    void tasksAreEqualIfIdsMatch() {
        Task task1 = new Task("Task", "Description", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Another Task", "Another Description", Status.DONE);
        task2.setId(1);
        assertEquals(task1, task2);
    }

    @Test
    void taskIsUnchangedAfterAdding() {
        Task task = new Task("Immutable", "Test", Status.NEW);
        task.setId(1);

        assertEquals("Immutable", task.getName());
        assertEquals("Test", task.getDescription());
        assertEquals(Status.NEW, task.getStatus());
    }

    @Test
    void taskIdShouldBeGeneratedAndNoConflictWithManualId() {
        Task manualIdTask = new Task("Manual Task", "Task with manual id", Status.NEW);
        manualIdTask.setId(100);

        taskManager.createTask(manualIdTask);
        int generatedId = manualIdTask.getId();

        assertNotEquals(100, generatedId, "Id должен быть сгенерирован заново, не равен 100");

        Task anotherTask = new Task("Another Task", "Another", Status.NEW);
        taskManager.createTask(anotherTask);
        int secondId = anotherTask.getId();

        assertNotEquals(generatedId, secondId, "Id задач не должны совпадать");

        List<Task> allTasks = taskManager.getAllTasks();
        assertEquals(2, allTasks.size(), "Должно быть 2 задачи в менеджере");
    }

    @Test
    void taskFieldsShouldNotChangeExceptIdAfterAdding() {
        Task originalTask = new Task("Test Task", "Description of task", Status.IN_PROGRESS);

        String originalName = originalTask.getName();
        String originalDescription = originalTask.getDescription();
        Status originalStatus = originalTask.getStatus();

        taskManager.createTask(originalTask);

        Task storedTask = taskManager.getTask(originalTask.getId());

        assertNotNull(storedTask, "Задача должна быть найдена в менеджере");

        assertEquals(originalName, storedTask.getName(), "Имя задачи должно остаться неизменным");
        assertEquals(originalDescription, storedTask.getDescription(), "Описание задачи должно остаться неизменным");
        assertEquals(originalStatus, storedTask.getStatus(), "Статус задачи должен остаться неизменным");

        assertTrue(storedTask.getId() > 0, "Id задачи должен быть сгенерирован");
    }
}