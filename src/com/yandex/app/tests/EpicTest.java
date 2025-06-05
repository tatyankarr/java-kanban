package com.yandex.app.tests;

import static org.junit.jupiter.api.Assertions.*;
import com.yandex.app.model.Epic;
import com.yandex.app.service.InMemoryHistoryManager;
import com.yandex.app.service.InMemoryTaskManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class EpicTest {
    private InMemoryTaskManager taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager(new InMemoryHistoryManager());
    }

    @Test
    void epicsAreEqualIfIdsMatch() {
        Epic epic1 = new Epic("Epic1", "Description");
        epic1.setId(2);
        Epic epic2 = new Epic("Epic2", "Other Description");
        epic2.setId(2);
        assertEquals(epic1, epic2);
    }

    @Test
    void epicCannotContainItselfAsSubtask() {
        Epic epic = new Epic("Epic", "Self-reference test");
        epic.setId(1);

        epic.addSubtask(1);

        assertFalse(epic.getSubtaskId().contains(1), "Epic не должен содержать самого себя в списке подзадач");
    }

    @Test
    void epicIdShouldBeGeneratedAndNoConflictWithManualId() {
        Epic manualIdEpic = new Epic("Manual Epic", "Epic with manual id");
        manualIdEpic.setId(100);

        taskManager.createEpic(manualIdEpic);
        int generatedId = manualIdEpic.getId();

        assertNotEquals(100, generatedId, "Id должен быть сгенерирован заново, не равен 100");

        Epic anotherEpic = new Epic("Another Epic", "Another epic");
        taskManager.createEpic(anotherEpic);
        int secondId = anotherEpic.getId();

        assertNotEquals(generatedId, secondId, "Id эпиков не должны совпадать");

        List<Epic> allEpics = taskManager.getAllEpics();
        assertEquals(2, allEpics.size(), "Должно быть 2 эпика в менеджере");
    }

    @Test
    void epicFieldsShouldNotChangeExceptIdAfterAdding() {
        Epic originalEpic = new Epic("Epic Title", "Epic Description");

        String originalName = originalEpic.getName();
        String originalDescription = originalEpic.getDescription();

        taskManager.createEpic(originalEpic);

        Epic storedEpic = taskManager.getEpic(originalEpic.getId());

        assertNotNull(storedEpic, "Эпик должен быть найден в менеджере");
        assertEquals(originalName, storedEpic.getName(), "Имя эпика должно остаться неизменным");
        assertEquals(originalDescription, storedEpic.getDescription(), "Описание эпика должно остаться неизменным");

        assertTrue(storedEpic.getId() > 0, "ID эпика должен быть установлен менеджером");
    }
}