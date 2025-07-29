package src;

import com.yandex.app.enums.Status;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.model.Task;
import com.yandex.app.service.FileBackedTaskManager;
import com.yandex.app.service.Managers;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    @Test
    void shouldSaveAndLoadEmptyManager() throws IOException {
        File tempFile = File.createTempFile("empty", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile, Managers.getDefaultHistory());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loaded.getAllTasks().isEmpty());
        assertTrue(loaded.getAllEpics().isEmpty());
        assertTrue(loaded.getAllSubtasks().isEmpty());
    }

    @Test
    void shouldSaveAndLoadMultipleTasks() throws IOException {
        File tempFile = File.createTempFile("multi", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile, Managers.getDefaultHistory());

        Task task1 = new Task("Task 1", "Description 1", Status.NEW);
        Task task2 = new Task("Task 2", "Description 2", Status.IN_PROGRESS);
        manager.createTask(task1);
        manager.createTask(task2);

        Epic epic = new Epic("Epic 1", "Epic description");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Subtask 1", "Linked to Epic", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("Subtask 2", "Also linked", Status.DONE, epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        List<Task> loadedTasks = loaded.getAllTasks();
        assertEquals(2, loadedTasks.size());
        assertEquals("Task 1", loadedTasks.get(0).getName());

        List<Epic> loadedEpics = loaded.getAllEpics();
        assertEquals(1, loadedEpics.size());
        assertEquals("Epic 1", loadedEpics.get(0).getName());

        List<Subtask> loadedSubtasks = loaded.getAllSubtasks();
        assertEquals(2, loadedSubtasks.size());
        assertEquals(epic.getId(), loadedSubtasks.get(0).getEpicId());
    }

    @Test
    void shouldPreserveDataAfterReload() throws IOException {
        File tempFile = File.createTempFile("persistence", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile, Managers.getDefaultHistory());

        Task task = new Task("Persistent Task", "To be saved", Status.DONE);
        manager.createTask(task);

        Epic epic = new Epic("Persistent Epic", "With one subtask");
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Persistent Subtask", "Linked", Status.NEW, epic.getId());
        manager.createSubtask(subtask);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getAllTasks().size());
        assertEquals("Persistent Task", loaded.getAllTasks().get(0).getName());

        assertEquals(1, loaded.getAllEpics().size());
        assertEquals("Persistent Epic", loaded.getAllEpics().get(0).getName());

        assertEquals(1, loaded.getAllSubtasks().size());
        assertEquals(epic.getId(), loaded.getAllSubtasks().get(0).getEpicId());
    }
}
