package com.yandex.app;

import com.yandex.app.model.*;
import com.yandex.app.enums.Status;
import com.yandex.app.service.Managers;
import com.yandex.app.interfaces.TaskManager;

public class Main {

    public static void main(String[] args) {
        TaskManager manager = Managers.getDefault();

        Task task1 = new Task("Починить кран", "Позвать сантехника", Status.NEW);
        Task task2 = new Task("Купить продукты", "Список: хлеб, молоко, сыр", Status.NEW);
        manager.createTask(task1);
        manager.createTask(task2);

        Epic epic1 = new Epic("Подготовка к отпуску", "Сделать всё до поездки");
        manager.createEpic(epic1);

        Subtask subtask1_1 = new Subtask("Купить билеты", "Авиабилеты туда-обратно", Status.NEW, epic1.getId());
        Subtask subtask1_2 = new Subtask("Собрать вещи", "Чемодан и документы", Status.NEW, epic1.getId());
        manager.createSubtask(subtask1_1);
        manager.createSubtask(subtask1_2);

        Epic epic2 = new Epic("Переезд", "Подготовка к переезду");
        manager.createEpic(epic2);

        Subtask subtask2_1 = new Subtask("Упаковать вещи", "Сложить вещи в коробки", Status.NEW, epic2.getId());
        manager.createSubtask(subtask2_1);

        printList(manager);

        manager.getTask(task1.getId());
        manager.getTask(task2.getId());
        manager.getEpic(epic1.getId());
        manager.getSubtask(subtask1_1.getId());
        manager.getSubtask(subtask1_2.getId());
        manager.getEpic(epic2.getId());
        manager.getSubtask(subtask2_1.getId());

        System.out.println();
        System.out.println("---История просмотров:---");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }

        task1.setStatus(Status.DONE);
        manager.updateTask(task1);

        subtask1_2.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask1_2);

        subtask2_1.setStatus(Status.DONE);
        manager.updateSubtask(subtask2_1);

        System.out.println();
        System.out.println("CТАТУСЫ ЗАДАЧ ИЗМЕНЕНЫ.");
        printList(manager);

        manager.removeEpic(6);
        manager.removeSubtask(4);

        System.out.println();
        System.out.println("ЗАДАЧИ УДАЛЕНЫ.");
        printList(manager);
    }

    private static void printList(TaskManager manager) {
        System.out.println();
        System.out.println("---ЗАДАЧИ---");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println();
        System.out.println("---ЭПИКИ---");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println(epic);
            for (Integer subId : epic.getSubtaskId()) {
                Subtask subtask = manager.getSubtaskWithoutHistory(subId);
                System.out.println(" - " + subtask);
            }
        }
    }
}
