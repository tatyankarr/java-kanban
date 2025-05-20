public class Main {

    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        Task task1 = new Task("Починить кран", "Позвать сантехника", Status.NEW);
        Task task2 = new Task ("Купить продукты", "Список: хлеб, молоко, сыр", Status.NEW);
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

        task1.setStatus(Status.DONE);
        manager.updateTask(task1);

        subtask1_2.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask1_2);

        subtask2_1.setStatus(Status.DONE);
        manager.updateSubtask(subtask2_1);

        System.out.println();
        System.out.println("CТАТУСЫ ЗАДАЧ ИЗМЕНЕНЫ.");
        printList(manager);

        manager.removeEpic(2);
        manager.removeSubtask(2);

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
                Subtask subtask = manager.getSubtask(subId);
                System.out.println(" - " + subtask);
            }
        }
    }

}
