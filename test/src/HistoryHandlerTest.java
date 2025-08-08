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
import com.yandex.app.model.Task;
import com.yandex.app.interfaces.TaskManager;
import com.yandex.app.service.HttpTaskServer;
import com.yandex.app.service.InMemoryHistoryManager;
import com.yandex.app.service.InMemoryTaskManager;
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

class HistoryHandlerTest {
    private HttpTaskServer taskServer;
    private TaskManager manager;
    private HttpClient client;
    private Gson gson;

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
        taskServer.start();
    }

    @AfterEach
    void tearDown() {
        taskServer.stop();
    }

    @Test
    void shouldReturnEmptyHistoryWhenNoTasksViewed() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        List<Task> history = gson.fromJson(response.body(), new TypeToken<List<Task>>() {}.getType());
        assertTrue(history.isEmpty(), "History should be empty when no tasks viewed");
    }

    @Test
    void shouldReturnHistoryInCorrectOrder() throws IOException, InterruptedException {
        Task task1 = new Task("Task 1", "Description", Status.NEW);
        Task task2 = new Task("Task 2", "Description", Status.IN_PROGRESS);
        Epic epic = new Epic("Epic", "Description");

        manager.createTask(task1);
        manager.createTask(task2);
        manager.createEpic(epic);

        manager.getTask(2);
        manager.getEpic(3);
        manager.getTask(1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        List<Task> history = gson.fromJson(response.body(), new TypeToken<List<Task>>() {}.getType());

        assertEquals(3, history.size(), "History should contain 3 items");
        assertEquals(2, history.get(0).getId(), "First item should be Task 2 (first viewed)");
        assertEquals(3, history.get(1).getId(), "Second item should be Epic (second viewed)");
        assertEquals(1, history.get(2).getId(), "Last item should be Task 1 (last viewed)");
    }

    @Test
    void shouldReturnFilteredHistoryAfterTaskDeletion() throws IOException, InterruptedException {
        Task task1 = new Task("Task 1", "Description", Status.NEW);
        Task task2 = new Task("Task 2", "Description", Status.DONE);

        manager.createTask(task1);
        manager.createTask(task2);

        manager.getTask(1);
        manager.getTask(2);

        manager.removeTask(1);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        List<Task> history = gson.fromJson(response.body(), new TypeToken<List<Task>>() {}.getType());

        assertEquals(1, history.size(), "History should contain only remaining task");
        assertEquals(2, history.get(0).getId(), "Remaining task should be Task 2");
    }

    @Test
    void shouldNotAllowPostRequestsToHistory() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(500, response.statusCode(), "POST method should not be allowed for history");
    }
}