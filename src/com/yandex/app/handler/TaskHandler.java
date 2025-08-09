package com.yandex.app.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.yandex.app.model.Task;
import com.yandex.app.interfaces.TaskManager;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

public class TaskHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public TaskHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Duration.class, new DurationTypeAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();

            switch (method) {
                case "GET":
                    handleGet(exchange, path);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, path);
                    break;
                default:
                    sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        String[] pathSegments = path.split("/");
        if (pathSegments.length == 3 && pathSegments[1].equals("tasks")) {
            try {
                int id = Integer.parseInt(pathSegments[2]);
                Task task = manager.getTask(id);
                if (task == null) {
                    sendNotFound(exchange);
                } else {
                    sendText(exchange, gson.toJson(task));
                }
            } catch (NumberFormatException e) {
                sendNotFound(exchange);
            }
        } else {
            sendText(exchange, gson.toJson(manager.getAllTasks()));
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        try {
            Task task = gson.fromJson(body, Task.class);
            if (task == null) {
                sendServerError(exchange);
                return;
            }
            if (task.getId() == 0 || manager.getTask(task.getId()) == null) {
                manager.createTask(task);
            } else {
                manager.updateTask(task);
            }
            sendCreated(exchange);
        } catch (com.google.gson.JsonSyntaxException | IllegalArgumentException e) {
            e.printStackTrace();
            sendHasInteractions(exchange);
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] pathSegments = path.split("/");
        if (pathSegments.length == 3 && pathSegments[1].equals("tasks")) {
            try {
                int id = Integer.parseInt(pathSegments[2]);
                if (manager.getTask(id) == null) {
                    sendNotFound(exchange);
                } else {
                    manager.removeTask(id);
                    sendText(exchange, "Task deleted");
                }
            } catch (NumberFormatException e) {
                sendNotFound(exchange);
            }
        } else {
            manager.clearAllTasks();
            sendText(exchange, "All tasks deleted");
        }
    }

    private static class DurationTypeAdapter extends TypeAdapter<Duration> {
        @Override
        public void write(JsonWriter out, Duration value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.getSeconds());
            }
        }

        @Override
        public Duration read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return Duration.ofSeconds(in.nextLong());
        }
    }

    private static class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString());
        }
    }
}