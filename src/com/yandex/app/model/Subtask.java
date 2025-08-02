package com.yandex.app.model;

import com.yandex.app.enums.Status;

public class Subtask extends Task {
    private int epicId;

    public void setEpicId(int epicId) {
        if (this.getId() == epicId) {
            throw new IllegalArgumentException("Subtask не может быть своим же эпиком: " + epicId);
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
                ", начало: " + getStartTime() +
                ", продолжительность: " + getDuration().toMinutes() + " мин" +
                ", эпик: " + getEpicId() + '}';
    }
}
