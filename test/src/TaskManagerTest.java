package src;

import com.yandex.app.enums.Status;
import com.yandex.app.interfaces.TaskManager;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {
    protected T taskManager;
    protected Task task;
    protected Epic epic;
    protected Subtask subtask;

    protected abstract T createTaskManager();

    @BeforeEach
    void setUp() {
        taskManager = createTaskManager();

        task = new Task("Test Task", "Description", Status.NEW);
        task.setStartTime(LocalDateTime.now());
        task.setDuration(Duration.ofMinutes(30));

        epic = new Epic("Test Epic", "Description");

        subtask = new Subtask("Test Subtask", "Description", Status.NEW, epic.getId());
        subtask.setStartTime(LocalDateTime.now().plusHours(1));
        subtask.setDuration(Duration.ofMinutes(15));
    }

    @Test
    void shouldCreateAndGetTask() {
        taskManager.createTask(task);
        final Task savedTask = taskManager.getTask(task.getId());

        assertNotNull(savedTask, "Задача не найдена");
        assertEquals(task, savedTask, "Задачи не совпадают");
    }

    @Test
    void shouldUpdateTask() {
        taskManager.createTask(task);
        task.setStatus(Status.IN_PROGRESS);
        taskManager.updateTask(task);

        assertEquals(Status.IN_PROGRESS, taskManager.getTask(task.getId()).getStatus(),
                "Статус задачи не обновился");
    }

    @Test
    void shouldDeleteTask() {
        taskManager.createTask(task);
        taskManager.removeTask(task.getId());

        assertNull(taskManager.getTask(task.getId()), "Задача не удалилась");
    }

    @Test
    void shouldCreateAndGetEpic() {
        taskManager.createEpic(epic);
        final Epic savedEpic = taskManager.getEpic(epic.getId());

        assertNotNull(savedEpic, "Эпик не найден");
        assertEquals(epic, savedEpic, "Эпики не совпадают");
    }

    @Test
    void shouldCalculateEpicStatus() {
        taskManager.createEpic(epic);

        Subtask sub1 = new Subtask("Sub1", "Desc", Status.NEW, epic.getId());
        taskManager.createSubtask(sub1);
        assertEquals(Status.NEW, epic.getStatus(), "Статус должен быть NEW");

        sub1.setStatus(Status.DONE);
        taskManager.updateSubtask(sub1);
        assertEquals(Status.DONE, epic.getStatus(), "Статус должен быть DONE");

        Subtask sub2 = new Subtask("Sub2", "Desc", Status.NEW, epic.getId());
        taskManager.createSubtask(sub2);
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Статус должен быть IN_PROGRESS");

        sub2.setStatus(Status.IN_PROGRESS);
        taskManager.updateSubtask(sub2);
        assertEquals(Status.IN_PROGRESS, epic.getStatus(), "Статус должен быть IN_PROGRESS");
    }

    @Test
    void shouldCreateAndGetSubtask() {
        Epic epic = new Epic("Test Epic", "Description");
        taskManager.createEpic(epic);

        Subtask subtask = new Subtask("Test Subtask", "Description", Status.NEW, epic.getId());
        taskManager.createSubtask(subtask);

        final Subtask savedSubtask = taskManager.getSubtask(subtask.getId());
        assertNotNull(savedSubtask, "Подзадача не найдена");
        assertEquals(epic.getId(), savedSubtask.getEpicId(),
                "Подзадача должна быть связана с эпиком");
    }

    @Test
    void shouldUpdateSubtaskAndEpic() {
        Epic epic = new Epic("Test Epic", "Description");
        taskManager.createEpic(epic);

        Subtask subtask = new Subtask("Test Subtask", "Description", Status.NEW, epic.getId());
        taskManager.createSubtask(subtask);

        subtask.setStatus(Status.IN_PROGRESS);
        taskManager.updateSubtask(subtask);

        Subtask updatedSubtask = taskManager.getSubtaskWithoutHistory(subtask.getId());
        assertNotNull(updatedSubtask, "Подзадача не найдена");
        assertEquals(Status.IN_PROGRESS, updatedSubtask.getStatus(),
                "Статус подзадачи не обновился");

        assertEquals(Status.IN_PROGRESS, taskManager.getEpic(epic.getId()).getStatus(),
                "Статус эпика не обновился");
    }

    @Test
    void shouldPrioritizeTasksByStartTime() {
        Task earlyTask = new Task("Early", "Desc", Status.NEW);
        earlyTask.setStartTime(LocalDateTime.now());

        Task lateTask = new Task("Late", "Desc", Status.NEW);
        lateTask.setStartTime(LocalDateTime.now().plusHours(1));

        taskManager.createTask(lateTask);
        taskManager.createTask(earlyTask);

        List<Task> prioritized = taskManager.getPrioritizedTasks();
        assertEquals(earlyTask, prioritized.get(0), "Неверный порядок задач");
    }

    @Test
    void shouldDetectTimeConflicts() {
        taskManager.createTask(task);

        Task conflictingTask = new Task("Conflict", "Desc", Status.NEW);
        conflictingTask.setStartTime(task.getStartTime().plusMinutes(15));
        conflictingTask.setDuration(Duration.ofMinutes(30));

        assertThrows(IllegalArgumentException.class, () ->
                        taskManager.createTask(conflictingTask),
                "Должно быть исключение при конфликте времени");
    }

    @Test
    void shouldMaintainHistoryWithoutDuplicates() {
        taskManager.createTask(task);
        taskManager.getTask(task.getId());
        taskManager.getTask(task.getId());

        assertEquals(1, taskManager.getHistory().size(),
                "История не должна содержать дубликатов");
    }
}