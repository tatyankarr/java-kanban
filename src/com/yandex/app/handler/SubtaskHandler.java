package com.yandex.app.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.yandex.app.model.Subtask;
import com.yandex.app.interfaces.TaskManager;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

public class SubtaskHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public SubtaskHandler(TaskManager manager) {
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
            String query = uri.getQuery();

            switch (method) {
                case "GET":
                    handleGet(exchange, query);
                    break;
                case "POST":
                    handlePost(exchange);
                    break;
                case "DELETE":
                    handleDelete(exchange, query);
                    break;
                default:
                    sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }

    private void handleGet(HttpExchange exchange, String query) throws IOException {
        if (query == null) {
            sendText(exchange, gson.toJson(manager.getAllSubtasks()));
        } else {
            int id = extractId(query);
            Subtask subtask = manager.getSubtask(id);
            if (subtask == null) {
                sendNotFound(exchange);
            } else {
                sendText(exchange, gson.toJson(subtask));
            }
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        try {
            Subtask subtask = gson.fromJson(body, Subtask.class);
            if (subtask == null) {
                sendServerError(exchange);
                return;
            }

            if (manager.getEpic(subtask.getEpicId()) == null) {
                sendHasInteractions(exchange);
                return;
            }

            if (subtask.getId() == 0 || manager.getSubtask(subtask.getId()) == null) {
                manager.createSubtask(subtask);
            } else {
                manager.updateSubtask(subtask);
            }
            sendCreated(exchange);
        } catch (com.google.gson.JsonSyntaxException | IllegalArgumentException e) {
            e.printStackTrace();
            sendHasInteractions(exchange);
        }
    }

    private void handleDelete(HttpExchange exchange, String query) throws IOException {
        if (query == null) {
            manager.clearAllSubtasks();
            sendText(exchange, "All subtasks deleted");
        } else {
            int id = extractId(query);
            if (manager.getSubtask(id) == null) {
                sendNotFound(exchange);
            } else {
                manager.removeSubtask(id);
                sendText(exchange, "Subtask deleted");
            }
        }
    }

    private int extractId(String query) {
        return Integer.parseInt(query.split("=")[1]);
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