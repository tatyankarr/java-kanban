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
    void addShouldKeepOnlyLastInstanceWhenAddingSameTaskMultipleTimes() {
        historyManager.add(task1);
        historyManager.add(task1);
        historyManager.add(task1);

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "History should contain only one instance of the task");
    }

    @Test
    void removeShouldDeleteTaskFromHistoryWhenTaskExists() {
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(task2.getId());
        List<Task> history = historyManager.getHistory();

        assertEquals(2, history.size());
        assertFalse(history.contains(task2), "History should not contain removed task");
        assertEquals(task1, history.get(0));
        assertEquals(task3, history.get(1));
    }

    @Test
    void removeShouldNotFailWhenRemovingFromEmptyHistory() {
        assertDoesNotThrow(() -> historyManager.remove(1));
    }

    @Test
    void removeShouldWorkCorrectlyWhenRemovingFromBeginningMiddleAndEnd() {
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        historyManager.remove(task1.getId());
        List<Task> history = historyManager.getHistory();
        assertEquals(2, history.size());
        assertEquals(task2, history.get(0));
        assertEquals(task3, history.get(1));

        historyManager.remove(task3.getId());
        history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task2, history.get(0));

        historyManager.remove(task2.getId());
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    void getHistoryShouldReturnEmptyListWhenNoTasksViewed() {
        assertTrue(historyManager.getHistory().isEmpty());
    }

    @Test
    void getHistoryShouldMaintainInsertionOrder() {
        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task3);

        List<Task> history = historyManager.getHistory();
        assertEquals(3, history.size());
        assertEquals(task1, history.get(0));
        assertEquals(task2, history.get(1));
        assertEquals(task3, history.get(2));
    }

    @Test
    void historyShouldReflectExternalChangesWhenNoCopyIsMade() {
        Task task = new Task("Original", "Original description", Status.NEW);
        historyManager.add(task);

        task.setName("Changed");
        task.setDescription("Changed description");
        task.setStatus(Status.DONE);

        List<Task> history = historyManager.getHistory();
        Task fromHistory = history.get(0);

        assertEquals("Changed", fromHistory.getName(),
                "Task name in history changed (should not happen if copy was made)");
        assertEquals("Changed description", fromHistory.getDescription());
        assertEquals(Status.DONE, fromHistory.getStatus());
    }
}