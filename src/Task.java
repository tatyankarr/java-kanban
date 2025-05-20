import java.util.Objects;

public class Task {
    protected int id;
    protected String name;
    protected String description;
    protected Status status;


    public Task(String name, String description, Status status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "Задача " + id + ": {" + name +
                " (" + description + "), статус: " + status +
                '}';
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
