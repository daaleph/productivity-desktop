package home.model;

public class Branch {
    protected int id;
    protected String name;

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
    protected List<Project> projects;

    public ProjectBranch(int id) {
        super(id);
        fetchData();
    }

    public abstract void fetchData();

    public List<Project> getProjects() {
        return projects;
    }
}