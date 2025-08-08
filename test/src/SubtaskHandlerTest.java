package src;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import com.yandex.app.enums.Status;
import com.yandex.app.model.Epic;
import com.yandex.app.model.Subtask;
import com.yandex.app.interfaces.TaskManager;
import com.yandex.app.service.InMemoryHistoryManager;
import com.yandex.app.service.InMemoryTaskManager;
import com.yandex.app.service.HttpTaskServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubtaskHandlerTest {
    private HttpTaskServer taskServer;
    private TaskManager manager;
    private HttpClient client;
    private Gson gson;
    private Epic testEpic;

    private static class DurationTypeAdapter extends TypeAdapter<Duration> {
        @Override
        public void write(JsonWriter out, Duration value) throws IOException {
            out.value(value == null ? null : value.toMinutes());
        }

        @Override
        public Duration read(JsonReader in) throws IOException {
            return in.peek() == JsonToken.NULL ? null : Duration.ofMinutes(in.nextLong());
        }
    }

    private static class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            out.value(value == null ? null : value.toString());
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            return in.peek() == JsonToken.NULL ? null : LocalDateTime.parse(in.nextString());
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        manager = new InMemoryTaskManager(new InMemoryHistoryManager());
        taskServer = new HttpTaskServer(manager);
        gson = new GsonBuilder()
                .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        client = HttpClient.newHttpClient();

        testEpic = new Epic("Test Epic", "Description");
        manager.createEpic(testEpic);

        taskServer.start();
    }

    @AfterEach
    void tearDown() {
        taskServer.stop();
    }

    @Test
    void shouldCreateSubtaskAndReturn201() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("New Subtask", "Description", Status.NEW, testEpic.getId());
        String subtaskJson = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertEquals(1, manager.getAllSubtasks().size());
        assertEquals("New Subtask", manager.getSubtask(2).getName());
    }

    @Test
    void shouldGetSubtaskByIdAndReturn200() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Test Subtask", "Description", Status.NEW, testEpic.getId());
        manager.createSubtask(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks?id=2"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Subtask receivedSubtask = gson.fromJson(response.body(), Subtask.class);
        assertEquals("Test Subtask", receivedSubtask.getName());
        assertEquals(testEpic.getId(), receivedSubtask.getEpicId());
    }

    @Test
    void shouldReturn404ForNonExistentSubtask() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks?id=999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldGetAllSubtasksAndReturn200() throws IOException, InterruptedException {
        manager.createSubtask(new Subtask("Subtask 1", "Desc 1", Status.NEW, testEpic.getId()));
        manager.createSubtask(new Subtask("Subtask 2", "Desc 2", Status.IN_PROGRESS, testEpic.getId()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        List<Subtask> subtasks = gson.fromJson(response.body(), new TypeToken<List<Subtask>>() {}.getType());
        assertEquals(2, subtasks.size());
    }

    @Test
    void shouldUpdateSubtaskAndReturn201() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Original", "Desc", Status.NEW, testEpic.getId());
        manager.createSubtask(subtask);
        subtask.setName("Updated");
        subtask.setStatus(Status.IN_PROGRESS);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(subtask)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        Subtask updatedSubtask = manager.getSubtask(subtask.getId());
        assertEquals("Updated", updatedSubtask.getName());
        assertEquals(Status.IN_PROGRESS, updatedSubtask.getStatus());
    }

    @Test
    void shouldDeleteSubtaskByIdAndReturn200() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Subtask to delete", "Desc", Status.NEW, testEpic.getId());
        manager.createSubtask(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks?id=2"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void shouldDeleteAllSubtasksAndReturn200() throws IOException, InterruptedException {
        manager.createSubtask(new Subtask("Subtask 1", "Desc 1", Status.NEW, testEpic.getId()));
        manager.createSubtask(new Subtask("Subtask 2", "Desc 2", Status.DONE, testEpic.getId()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    void shouldReturn406WhenCreatingSubtaskWithInvalidEpic() throws IOException, InterruptedException {
        assertNull(manager.getEpic(999), "Epic with ID=999 should not exist");

        Subtask subtask = new Subtask("Invalid Subtask", "Desc", Status.NEW, 999);
        String subtaskJson = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode(), "Should return 406 for subtask with non-existent epic");

        assertEquals(0, manager.getAllSubtasks().size(), "No subtasks should be created");
    }

    @Test
    void shouldUpdateEpicStatusWhenSubtaskChanges() throws IOException, InterruptedException {
        Subtask subtask = new Subtask("Subtask", "Desc", Status.NEW, testEpic.getId());
        manager.createSubtask(subtask);

        subtask.setStatus(Status.DONE);
        String subtaskJson = gson.toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());

        assertEquals(Status.DONE, manager.getEpic(testEpic.getId()).getStatus());
    }
}
