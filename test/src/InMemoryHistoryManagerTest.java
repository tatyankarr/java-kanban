package src;

import com.yandex.app.enums.Status;
import com.yandex.app.model.Task;
import com.yandex.app.service.InMemoryHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {
    private InMemoryHistoryManager historyManager;
    private Task task1;
    private Task task2;
    private Task task3;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
        task1 = new Task("Task 1", "Description 1", Status.NEW);
        task1.setId(1);
        task2 = new Task("Task 2", "Description 2", Status.IN_PROGRESS);
        task2.setId(2);
        task3 = new Task("Task 3", "Description 3", Status.DONE);
        task3.setId(3);
    }

    @Test
    void shouldAddTasksToHistory() {
        historyManager.add(task1);
        historyManager.add(task2);

        final List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size(), "Неверный размер истории");
        assertEquals(task1, history.get(0), "Первая задача не совпадает");
        assertEquals(task2, history.get(1), "Вторая задача не совпадает");
    }

    @Test
    void shouldNotDuplicateTasksInHistory() {
        historyManager.add(task1);
        historyManager.add(task1);

        assertEquals(1, historyManager.getHistory().size(),
                "История не должна содержать дубликатов");
    }

    @Test
    void shouldRemoveTasksFromHistory() {
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(task2.getId());
        assertFalse(historyManager.getHistory().contains(task2),
                "Задача должна удалиться из истории");

        historyManager.remove(task1.getId());
        assertFalse(historyManager.getHistory().contains(task1),
                "Задача должна удалиться из истории");

        historyManager.remove(task3.getId());
        assertTrue(historyManager.getHistory().isEmpty(),
                "История должна быть пустой");
    }

    @Test
    void shouldMaintainInsertionOrder() {
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        final List<Task> history = historyManager.getHistory();
        assertEquals(task1, history.get(0), "Неверный порядок задач");
        assertEquals(task2, history.get(1), "Неверный порядок задач");
        assertEquals(task3, history.get(2), "Неверный порядок задач");
    }
}