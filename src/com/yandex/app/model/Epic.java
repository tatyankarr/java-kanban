package com.yandex.app.model;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Epic extends Task {
    private final List<Integer> subtaskId = new ArrayList<>();
    private Duration duration = Duration.ZERO;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Epic(String name, String description) {
        super(name, description);
    }

    public List<Integer> getSubtaskId() {
        return new ArrayList<>(subtaskId);
    }

    public void addSubtask(int subtaskId) {
        if (this.getId() == subtaskId) {
            System.out.println("Epic не может быть своим же подзадачным ID: " + subtaskId);
            return;
        }
        this.subtaskId.add(subtaskId);
    }

    public void clearSubtask() {
        subtaskId.clear();
    }

    public void deleteSubstackId(int id) {
        subtaskId.remove((Integer) id);
    }

    public void updateTimeFields(Map<Integer, Subtask> subtaskMap) {
        List<Subtask> subtasks = getSubtaskId().stream()
                .map(subtaskMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (subtasks.isEmpty()) {
            startTime = null;
            endTime = null;
            duration = Duration.ZERO;
            return;
        }

        startTime = subtasks.stream()
                .map(Subtask::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        endTime = subtasks.stream()
                .map(Subtask::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        duration = subtasks.stream()
                .map(Subtask::getDuration)
                .filter(Objects::nonNull)
                .reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    public String toString() {
        return "Эпик " + getId() + ": {" + getName() +
                " (" + getDescription() + "), статус: " + getStatus() +
                ", начало: " + getStartTime() +
                ", продолжительность: " + getDuration().toMinutes() + " мин" +
                ", окончание: " + getEndTime() + '}';
    }

    @Override
    public Duration getDuration() {
        return duration;
    }

    @Override
    public LocalDateTime getStartTime() {
        return startTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        return endTime;
    }
}
