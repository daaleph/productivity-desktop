package home.models;

import java.util.Map;
import java.util.UUID;

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

abstract class ProjectedBranch extends Branch {
    private Map<UUID, Project> projects;

    public ProjectedBranch(int id) {
        super(id);
        fetchData();
    }

    public abstract void fetchData();

    public Map<UUID, Project> getProjects() {
        return projects;
    }
}