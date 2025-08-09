package com.yandex.app.model;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Epic extends Task {
    @Expose
    private final List<Integer> subtaskId = new ArrayList<>();
    @Expose
    private Duration epicDuration = Duration.ZERO;

    public Epic(String name, String description) {
        super(name, description);
    }

    public List<Integer> getSubtaskId() {
        return new ArrayList<>(subtaskId != null ? subtaskId : List.of());
    }

    public void addSubtask(int subtaskId) {
        if (this.getId() == subtaskId) {
            System.out.println("Epic не может быть своим же подзадачным ID: " + subtaskId);
            return;
        }
        safeList().add(subtaskId);
    }

    public void clearSubtask() {
        safeList().clear();
    }

    public void deleteSubstackId(int id) {
        safeList().remove((Integer) id);
    }

    private List<Integer> safeList() {
        return subtaskId != null ? subtaskId : new ArrayList<>();
    }

    public void updateTimeFields(Map<Integer, Subtask> subtaskMap) {
        List<Subtask> subtasks = getSubtaskId().stream()
                .map(subtaskMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (subtasks.isEmpty()) {
            setStartTime(null);
            setDuration(Duration.ZERO);
            return;
        }

        LocalDateTime minStart = subtasks.stream()
                .map(Subtask::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime maxEnd = subtasks.stream()
                .map(Subtask::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (minStart != null && maxEnd != null) {
            setStartTime(minStart);
            setDuration(Duration.between(minStart, maxEnd));
        } else {
            setStartTime(null);
            setDuration(Duration.ZERO);
        }
    }
}