package com.yandex.app.tests;

import com.yandex.app.model.Epic;
import com.yandex.app.service.InMemoryHistoryManager;
import com.yandex.app.service.InMemoryTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.yandex.app.enums.Status;
import com.yandex.app.model.Subtask;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubtaskTest {
    private InMemoryTaskManager taskManager;
    private int epicId;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager(new InMemoryHistoryManager());
        Epic epic = new Epic("Epic for Subtasks", "Epic Description");
        taskManager.createEpic(epic);
        epicId = epic.getId();
    }

    @Test
    void subtasksAreEqualIfIdsMatch() {
        Subtask sub1 = new Subtask("Subtask", "Desc", Status.NEW, 10);
        sub1.setId(5);
        Subtask sub2 = new Subtask("Other", "Other", Status.DONE, 10);
        sub2.setId(5);
        assertEquals(sub1, sub2);
    }

    @Test
    void subtaskCannotBeItsOwnEpic() {
        Subtask subtask = new Subtask("Subtask title", "desc", Status.NEW, 0);
        subtask.setId(5);
        subtask.setEpicId(5);
        assertNotEquals(subtask.getId(), subtask.getEpicId(), "Subtask не может быть своим же эпиком");
    }

    @Test
    void subtaskIdShouldBeGeneratedAndNoConflictWithManualId() {
        Subtask manualIdSubtask = new Subtask("Manual Subtask", "Subtask with manual id", Status.NEW, epicId);
        manualIdSubtask.setId(100);

        taskManager.createSubtask(manualIdSubtask);
        int generatedId = manualIdSubtask.getId();

        assertNotEquals(100, generatedId, "Id должен быть сгенерирован заново, не равен 100");

        Subtask anotherSubtask = new Subtask("Another Subtask", "Another subtask", Status.NEW, epicId);
        taskManager.createSubtask(anotherSubtask);
        int secondId = anotherSubtask.getId();

        assertNotEquals(generatedId, secondId, "Id подзадач не должны совпадать");

        List<Subtask> allSubtasks = taskManager.getAllSubtasks();
        assertEquals(2, allSubtasks.size(), "Должно быть 2 подзадачи в менеджере");
    }

    @Test
    void subtaskFieldsShouldNotChangeExceptIdAfterAdding() {
        Epic epic = new Epic("Main Epic", "Epic for Subtask");
        taskManager.createEpic(epic);
        int epicId = epic.getId();

        Subtask originalSubtask = new Subtask("Subtask Title", "Subtask Description", Status.NEW, epicId);

        String originalName = originalSubtask.getName();
        String originalDescription = originalSubtask.getDescription();
        Status originalStatus = originalSubtask.getStatus();
        int originalEpicId = originalSubtask.getEpicId();

        taskManager.createSubtask(originalSubtask);

        Subtask storedSubtask = taskManager.getSubtask(originalSubtask.getId());

        assertNotNull(storedSubtask, "Подзадача должна быть найдена в менеджере");
        assertEquals(originalName, storedSubtask.getName(), "Имя подзадачи должно остаться неизменным");
        assertEquals(originalDescription, storedSubtask.getDescription(), "Описание подзадачи должно остаться неизменным");
        assertEquals(originalStatus, storedSubtask.getStatus(), "Статус подзадачи должен остаться неизменным");
        assertEquals(originalEpicId, storedSubtask.getEpicId(), "ID эпика у подзадачи должен остаться прежним");
        assertTrue(storedSubtask.getId() > 0, "ID подзадачи должен быть установлен менеджером");
    }
}