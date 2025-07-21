package src;

import com.yandex.app.enums.Status;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.service.InMemoryTaskManager;
import com.yandex.app.service.InMemoryHistoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {

    private InMemoryTaskManager taskManager;
    private Epic epic;

    @BeforeEach
    void setUp() {
        taskManager = new InMemoryTaskManager(new InMemoryHistoryManager());
        epic = new Epic("Test Epic", "Epic Description");
        taskManager.createEpic(epic);
    }

    @Test
    void updateTaskShouldNotAffectManagerWhenTaskIdDoesNotExist() {
        Task task = new Task("Task", "Description", Status.NEW);
        task.setId(999); // non-existent ID
        assertDoesNotThrow(() -> taskManager.updateTask(task));
    }

    @Test
    void updateEpicShouldNotAffectManagerWhenEpicIdDoesNotExist() {
        Epic newEpic = new Epic("New Epic", "New Description");
        newEpic.setId(999); // non-existent ID
        assertDoesNotThrow(() -> taskManager.updateEpic(newEpic));
    }

    @Test
    void updateSubtaskShouldNotAffectManagerWhenSubtaskIdDoesNotExist() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, epic.getId());
        subtask.setId(999); // non-existent ID
        assertDoesNotThrow(() -> taskManager.updateSubtask(subtask));
    }

    @Test
    void removeSubtaskShouldAlsoRemoveItFromEpic() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, epic.getId());
        taskManager.createSubtask(subtask);

        taskManager.removeSubtask(subtask.getId());
        assertFalse(epic.getSubtaskId().contains(subtask.getId()));
    }

    @Test
    void clearAllSubtasksShouldRemoveThemFromAllEpics() {
        Subtask subtask1 = new Subtask("Subtask 1", "Desc 1", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Subtask 2", "Desc 2", Status.IN_PROGRESS, epic.getId());
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);

        taskManager.clearAllSubtasks();
        assertTrue(epic.getSubtaskId().isEmpty());
    }

    @Test
    void removeEpicShouldAlsoRemoveAllItsSubtasks() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, epic.getId());
        taskManager.createSubtask(subtask);

        taskManager.removeEpic(epic.getId());
        assertNull(taskManager.getSubtask(subtask.getId()));
    }

    @Test
    void epicStatusShouldBeNewWhenAllSubtasksAreNew() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.NEW, epic.getId());
        taskManager.createSubtask(subtask);
        assertEquals(Status.NEW, epic.getStatus());
    }

    @Test
    void epicStatusShouldBeDoneWhenAllSubtasksAreDone() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.DONE, epic.getId());
        taskManager.createSubtask(subtask);
        assertEquals(Status.DONE, epic.getStatus());
    }

    @Test
    void epicStatusShouldBeInProgressWhenSubtasksHaveMixedStatuses() {
        Subtask subtask1 = new Subtask("Subtask 1", "Desc 1", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Subtask 2", "Desc 2", Status.DONE, epic.getId());
        taskManager.createSubtask(subtask1);
        taskManager.createSubtask(subtask2);
        assertEquals(Status.IN_PROGRESS, epic.getStatus());
    }

    @Test
    void epicStatusShouldBeInProgressWhenSubtaskIsInProgress() {
        Subtask subtask = new Subtask("Subtask", "Description", Status.IN_PROGRESS, epic.getId());
        taskManager.createSubtask(subtask);
        assertEquals(Status.IN_PROGRESS, epic.getStatus());
    }

    @Test
    void getHistoryShouldNotContainRemovedTasks() {
        Task task = new Task("Task", "Description", Status.NEW);
        taskManager.createTask(task);
        taskManager.removeTask(task.getId());
        assertTrue(taskManager.getHistory().isEmpty());
    }

    @Test
    void getHistoryShouldReturnTasksInOrderOfViewing() {
        Task task1 = new Task("Task 1", "Desc 1", Status.NEW);
        Task task2 = new Task("Task 2", "Desc 2", Status.NEW);
        taskManager.createTask(task1);
        taskManager.createTask(task2);

        taskManager.getTask(task1.getId());
        taskManager.getTask(task2.getId());
        taskManager.getTask(task1.getId());

        List<Task> history = taskManager.getHistory();
        assertEquals(2, history.size());
        assertEquals(task2, history.get(0));
        assertEquals(task1, history.get(1));
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
