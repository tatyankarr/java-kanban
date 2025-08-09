package src;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import com.yandex.app.enums.Status;
import com.yandex.app.model.*;
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

class TaskHandlerTest {
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
    void shouldCreateTaskAndReturn201() throws IOException, InterruptedException {
        Task task = new Task("Task 1", "Description", Status.NEW);
        task.setDuration(Duration.ofMinutes(30));
        task.setStartTime(LocalDateTime.now());
        String taskJson = gson.toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response.statusCode());
        assertEquals(1, manager.getAllTasks().size());
        assertEquals("Task 1", manager.getTask(1).getName());
    }

    @Test
    void shouldGetTaskByIdAndReturn200() throws IOException, InterruptedException {
        Task task = new Task("Task 1", "Description", Status.NEW);
        task.setDuration(Duration.ofMinutes(30));
        task.setStartTime(LocalDateTime.now());
        manager.createTask(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/1"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        Task receivedTask = gson.fromJson(response.body(), Task.class);
        assertEquals(task.getName(), receivedTask.getName());
    }

    @Test
    void shouldReturn404ForNonExistentTask() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    void shouldGetAllTasksAndReturn200() throws IOException, InterruptedException {
        Task task1 = new Task("Task 1", "Desc 1", Status.NEW);
        task1.setDuration(Duration.ofMinutes(10));
        task1.setStartTime(LocalDateTime.now());
        manager.createTask(task1);

        Task task2 = new Task("Task 2", "Desc 2", Status.NEW);
        task2.setDuration(Duration.ofMinutes(20));
        task2.setStartTime(LocalDateTime.now().plusHours(1));
        manager.createTask(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        List<Task> tasks = gson.fromJson(response.body(), new TypeToken<List<Task>>() {}.getType());
        assertEquals(2, tasks.size(), "Should return 2 tasks");
        assertTrue(tasks.stream().anyMatch(t -> t.getName().equals("Task 1")));
        assertTrue(tasks.stream().anyMatch(t -> t.getName().equals("Task 2")));
    }

    @Test
    void shouldDeleteTaskByIdAndReturn200() throws IOException, InterruptedException {
        Task task = new Task("Task to delete", "Desc", Status.NEW);
        task.setDuration(Duration.ofMinutes(10));
        task.setStartTime(LocalDateTime.now());
        manager.createTask(task);

        assertEquals(1, manager.getAllTasks().size());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks?id=1"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllTasks().isEmpty(), "Task list should be empty after deletion");
    }

    @Test
    void shouldReturn406WhenCreatingOverlappingTask() throws IOException, InterruptedException {
        LocalDateTime startTime = LocalDateTime.of(2025, 8, 10, 10, 0);

        Task existingTask = new Task("Existing Task", "Desc", Status.NEW);
        existingTask.setDuration(Duration.ofMinutes(60));
        existingTask.setStartTime(startTime);
        manager.createTask(existingTask);

        Task overlappingTask = new Task("Overlapping Task", "Desc", Status.NEW);
        overlappingTask.setDuration(Duration.ofMinutes(30));
        overlappingTask.setStartTime(startTime.plusMinutes(30));

        String taskJson = gson.toJson(overlappingTask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, response.statusCode(), "Server should return 406 for overlapping tasks");

        assertEquals(1, manager.getAllTasks().size(), "Only one task should exist");
    }
}
