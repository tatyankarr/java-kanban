package com.yandex.app.tests;

import com.yandex.app.enums.Status;
import com.yandex.app.model.Task;
import com.yandex.app.service.InMemoryHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {

    private InMemoryHistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
    }

    @Test
    void shouldReflectExternalChangesIfNoCopyIsMade() {
        Task task = new Task("Original", "Original description", Status.NEW);

        historyManager.add(task);

        task.setName("Changed");
        task.setDescription("Changed description");
        task.setStatus(Status.DONE);

        List<Task> history = historyManager.getHistory();
        Task fromHistory = history.get(0);

        assertEquals("Changed", fromHistory.getName(), "Имя задачи в истории изменилось (так быть не должно, если копируется)");
        assertEquals("Changed description", fromHistory.getDescription());
        assertEquals(Status.DONE, fromHistory.getStatus());
    }
}