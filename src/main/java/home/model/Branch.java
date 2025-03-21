package home.model;

import java.util.Map;

public class Branch {
    private final int id;
    private String name;

    public Branch(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Name: %s", id, name);
    }

}

public abstract class ProjectBranch extends Branch {
    private Map<UUID, Project> projects;

    public ProjectBranch(int id) {
        super(id);
        fetchData();
    }

    public abstract void fetchData();

    public Map<UUID, Project> getProjects() {
        return projects;
    }
}