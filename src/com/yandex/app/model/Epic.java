package com.yandex.app.model;

import java.util.ArrayList;

public class Epic extends Task {
    private final ArrayList<Integer> subtaskId = new ArrayList<>();

    public Epic(String name, String description) {
        super(name, description);
    }

    public ArrayList<Integer> getSubtaskId() {
        return new ArrayList<>(subtaskId);
    }

    public void addSubtask(int id) {
        subtaskId.add(id);
    }

    public void clearSubtask() {
        subtaskId.clear();
    }

    public void deleteSubstackId(int id) {
        subtaskId.remove((Integer) id);
    }

    @Override
    public String toString() {
        return "Эпик " + getId() + ": {" + getName() +
                " (" + getDescription() + "), статус: " + getStatus() +
                '}';
    }
}
