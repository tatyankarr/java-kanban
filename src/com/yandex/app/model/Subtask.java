package com.yandex.app.model;

import com.yandex.app.enums.Status;

public class Subtask extends Task {
    private int epicId;

    public void setEpicId(int epicId) {
        if (this.getId() == epicId) {
            System.out.println("Subtask не может быть своим же эпиком: " + epicId);
            return;
        }
        this.epicId = epicId;
    }

    public Subtask(String name, String description, Status status, int epicId) {
        super(name, description, status);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    @Override
    public String toString() {
        return "Подзадача " + getId() + ": {" + getName() +
                " (" + getDescription() + "), статус: " + getStatus() +
                '}';
    }
}
