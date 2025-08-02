package com.yandex.app;

import com.yandex.app.model.*;
import com.yandex.app.enums.Status;
import com.yandex.app.service.Managers;
import com.yandex.app.service.FileBackedTaskManager;
import com.yandex.app.interfaces.TaskManager;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        File file = new File("tasks.csv");
        TaskManager manager = new FileBackedTaskManager(file, Managers.getDefaultHistory());

        Task task1 = new Task("Починить кран", "Позвать сантехника", Status.NEW);
        task1.setStartTime(LocalDateTime.now());
        task1.setDuration(Duration.ofHours(2));

        Task task2 = new Task("Купить продукты", "Список: хлеб, молоко, сыр", Status.NEW);
        task2.setStartTime(LocalDateTime.now().plusHours(3));
        task2.setDuration(Duration.ofMinutes(30));

        manager.createTask(task1);
        manager.createTask(task2);

        Epic epic1 = new Epic("Подготовка к отпуску", "Сделать всё до поездки");
        manager.createEpic(epic1);

        Subtask subtask1ForEpic1 = new Subtask("Купить билеты", "Авиабилеты туда-обратно", Status.NEW, epic1.getId());
        subtask1ForEpic1.setStartTime(LocalDateTime.now().plusDays(1));
        subtask1ForEpic1.setDuration(Duration.ofHours(1));

        Subtask subtask2ForEpic1 = new Subtask("Собрать вещи", "Чемодан и документы", Status.NEW, epic1.getId());
        subtask2ForEpic1.setStartTime(LocalDateTime.now().plusDays(1).plusHours(2));
        subtask2ForEpic1.setDuration(Duration.ofHours(3));

        manager.createSubtask(subtask1ForEpic1);
        manager.createSubtask(subtask2ForEpic1);

        Epic epic2 = new Epic("Переезд", "Подготовка к переезду");
        manager.createEpic(epic2);

        Subtask subtask1ForEpic2 = new Subtask("Упаковать вещи", "Сложить вещи в коробки", Status.NEW, epic2.getId());
        subtask1ForEpic2.setStartTime(LocalDateTime.now().plusDays(2));
        subtask1ForEpic2.setDuration(Duration.ofHours(4));

        manager.createSubtask(subtask1ForEpic2);

        printList(manager);
        TaskManager loadedManager = FileBackedTaskManager.loadFromFile(file);
        System.out.println("\n--- Восстановленный менеджер ---");
        printList(loadedManager);

        manager.getTask(task1.getId());
        manager.getTask(task2.getId());
        manager.getTask(task1.getId());
        manager.getEpic(epic1.getId());
        manager.getSubtask(subtask1ForEpic1.getId());
        manager.getSubtask(subtask2ForEpic1.getId());
        manager.getEpic(epic2.getId());
        manager.getSubtask(subtask1ForEpic2.getId());
        manager.getSubtask(subtask2ForEpic1.getId());

        System.out.println();
        System.out.println("---История просмотров:---");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }

        task1.setStatus(Status.DONE);
        manager.updateTask(task1);

        subtask2ForEpic1.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask2ForEpic1);

        subtask1ForEpic2.setStatus(Status.DONE);
        manager.updateSubtask(subtask1ForEpic2);

        System.out.println();
        System.out.println("CТАТУСЫ ЗАДАЧ ИЗМЕНЕНЫ.");
        printList(manager);

        manager.removeEpic(6);
        manager.removeSubtask(4);

        System.out.println();
        System.out.println("ЗАДАЧИ УДАЛЕНЫ.");
        printList(manager);

        System.out.println();
        System.out.println("---История просмотров:---");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }
    }

    private static void printList(TaskManager manager) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        System.out.println("\n=== ЗАДАЧИ ===");
        manager.getAllTasks().forEach(task -> {
            String startTime = task.getStartTime() != null
                    ? task.getStartTime().format(formatter)
                    : "не назначено";
            String duration = task.getDuration() != null
                    ? String.format("%d ч %02d мин",
                    task.getDuration().toHours(),
                    task.getDuration().toMinutesPart())
                    : "не указана";

            System.out.printf("Задача %d: %s (%s)%n",
                    task.getId(), task.getName(), task.getDescription());
            System.out.printf("  Статус: %s | Начало: %s | Длительность: %s%n%n",
                    task.getStatus(), startTime, duration);
        });

        System.out.println("\n=== ЭПИКИ ===");
        manager.getAllEpics().forEach(epic -> {
            Map<Integer, Subtask> subtasksMap = manager.getAllSubtasks().stream()
                    .collect(Collectors.toMap(Subtask::getId, subtask -> subtask));

            epic.updateTimeFields(subtasksMap);

            String startTime = epic.getStartTime() != null
                    ? epic.getStartTime().format(formatter)
                    : "не назначено";
            String endTime = epic.getEndTime() != null
                    ? epic.getEndTime().format(formatter)
                    : "не определено";
            String duration = epic.getDuration() != null
                    ? String.format("%d ч %02d мин",
                    epic.getDuration().toHours(),
                    epic.getDuration().toMinutesPart())
                    : "не указана";

            System.out.printf("Эпик %d: %s (%s)%n",
                    epic.getId(), epic.getName(), epic.getDescription());
            System.out.printf("  Статус: %s | Начало: %s | Окончание: %s | Длительность: %s%n",
                    epic.getStatus(), startTime, endTime, duration);

            System.out.println("  Подзадачи:");
            epic.getSubtaskId().forEach(subId -> {
                Subtask subtask = manager.getSubtaskWithoutHistory(subId);
                if (subtask != null) {
                    String subtaskStart = subtask.getStartTime() != null
                            ? subtask.getStartTime().format(formatter)
                            : "не назначено";
                    String subtaskDuration = subtask.getDuration() != null
                            ? String.format("%d ч %02d мин",
                            subtask.getDuration().toHours(),
                            subtask.getDuration().toMinutesPart())
                            : "не указана";

                    System.out.printf("  - Подзадача %d: %s (%s)%n",
                            subtask.getId(), subtask.getName(), subtask.getDescription());
                    System.out.printf("    Статус: %s | Начало: %s | Длительность: %s%n",
                            subtask.getStatus(), subtaskStart, subtaskDuration);
                }
            });
            System.out.println();
        });

        System.out.println("\n=== ПРИОРИТЕТНЫЕ ЗАДАЧИ ===");
        manager.getPrioritizedTasks().forEach(task -> {
            String timeInfo = task.getStartTime() != null
                    ? task.getStartTime().format(formatter) + " - " +
                    task.getStartTime().plus(task.getDuration()).format(formatter)
                    : "Время не назначено";
            System.out.printf("[%s] %s (%s)%n",
                    timeInfo, task.getName(), task.getStatus());
        });
    }
}
