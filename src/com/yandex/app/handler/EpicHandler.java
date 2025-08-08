package com.yandex.app.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import com.sun.net.httpserver.HttpExchange;
import com.yandex.app.model.Epic;
import com.yandex.app.interfaces.TaskManager;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

public class EpicHandler extends BaseHttpHandler {
    private final TaskManager manager;
    private final Gson gson;

    public EpicHandler(TaskManager manager) {
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
                    if (query == null) {
                        sendText(exchange, gson.toJson(manager.getAllEpics()));
                    } else {
                        int id = Integer.parseInt(query.split("=")[1]);
                        Epic epic = manager.getEpic(id);
                        if (epic == null) {
                            sendNotFound(exchange);
                        } else {
                            sendText(exchange, gson.toJson(epic));
                        }
                    }
                    break;
                case "POST":
                    String body = readRequestBody(exchange);
                    try {
                        Epic epic = gson.fromJson(body, Epic.class);
                        if (epic == null) {
                            sendServerError(exchange);
                            return;
                        }
                        if (epic.getId() == 0 || manager.getEpic(epic.getId()) == null) {
                            manager.createEpic(epic);
                            sendCreated(exchange);
                        } else {
                            manager.updateEpic(epic);
                            sendCreated(exchange);
                        }
                    } catch (com.google.gson.JsonSyntaxException e) {
                        e.printStackTrace();
                        sendServerError(exchange);
                    }
                    break;
                case "DELETE":
                    if (query == null) {
                        manager.clearAllEpics();
                        sendText(exchange, "All epics deleted");
                    } else {
                        int id = Integer.parseInt(query.split("=")[1]);
                        if (manager.getEpic(id) == null) {
                            sendNotFound(exchange);
                        } else {
                            manager.removeEpic(id);
                            sendText(exchange, "Epic deleted");
                        }
                    }
                    break;
                default:
                    sendServerError(exchange);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
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