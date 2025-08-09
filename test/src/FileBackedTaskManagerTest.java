package src;

import com.yandex.app.enums.Status;
import com.yandex.app.exceptions.ManagerSaveException;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.service.FileBackedTaskManager;
import com.yandex.app.service.InMemoryHistoryManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest extends TaskManagerTest<FileBackedTaskManager> {
    private File tempFile;

    @Override
    protected FileBackedTaskManager createTaskManager() {
        try {
            tempFile = Files.createTempFile("tasks", ".csv").toFile();
            return new FileBackedTaskManager(tempFile, new InMemoryHistoryManager());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file", e);
        }
    }

    @Test
    void shouldSaveAndLoadEmptyManager() {
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);
        assertTrue(loaded.getAllTasks().isEmpty(), "Загруженный менеджер должен быть пустым");
    }

    @Test
    void shouldSaveAndRestoreTasks() {
        taskManager.createEpic(epic);

        Subtask subtask = new Subtask("Test Subtask", "Description", Status.NEW, epic.getId());
        subtask.setStartTime(LocalDateTime.now().plusHours(1));
        subtask.setDuration(Duration.ofMinutes(15));

        taskManager.createTask(task);
        taskManager.createSubtask(subtask);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getAllTasks().size(), "Неверное количество задач");
        assertEquals(1, loaded.getAllEpics().size(), "Неверное количество эпиков");

        List<Subtask> subtasks = loaded.getAllSubtasks();
        assertEquals(1, subtasks.size(), "Неверное количество подзадач");

        Subtask loadedSubtask = subtasks.get(0);
        assertEquals(epic.getId(), loadedSubtask.getEpicId(),
                "Подзадача должна быть связана с эпиком");
    }

    @Test
    void shouldHandleFileErrors() {
        File invalidFile = new File("/invalid/path/tasks.csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(invalidFile, new InMemoryHistoryManager());

        Task task = new Task("Test", "Desc", Status.NEW);

        assertThrows(ManagerSaveException.class, () -> manager.createTask(task),
                "Должно быть исключение при ошибке записи в файл"
        );
    }
}
