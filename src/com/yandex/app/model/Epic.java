package com.yandex.app.model;

import java.util.ArrayList;
import java.util.List;

public class Epic extends Task {
    private final List<Integer> subtaskId = new ArrayList<>();

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

    @Override
    public String toString() {
        return "Эпик " + getId() + ": {" + getName() +
                " (" + getDescription() + "), статус: " + getStatus() +
                '}';
    }

}
