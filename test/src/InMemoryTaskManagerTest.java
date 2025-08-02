package src;

import com.yandex.app.enums.Status;
import com.yandex.app.model.Task;
import com.yandex.app.service.InMemoryTaskManager;
import com.yandex.app.service.InMemoryHistoryManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest extends TaskManagerTest<InMemoryTaskManager> {

    @Override
    protected InMemoryTaskManager createTaskManager() {
        return new InMemoryTaskManager(new InMemoryHistoryManager());
    }

    @Test
    void shouldGenerateUniqueIds() {
        taskManager.createTask(new Task("Task1", "Desc", Status.NEW));
        taskManager.createTask(new Task("Task2", "Desc", Status.NEW));

        assertNotEquals(
                taskManager.getAllTasks().get(0).getId(),
                taskManager.getAllTasks().get(1).getId(),
                "ID задач должны быть уникальными"
        );
    }
}
