package model.branchs;

import java.util.List;

import model.projects.Project;
import records.ProjectedBranchData;

public class Branch {
    protected final Integer id;
    private String name;

    public Branch(int id) {
        this.id = id;
    }

    public Branch(int id, String name) {
        this.name = name;
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
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
    protected ProjectedBranchData projects;

    public ProjectedBranch(int id, Object value) {
        super(id);
        addBelonging(value);
    }

    public void addBelonging(Object value) {
        if (!(value instanceof String || value instanceof Integer)) {
            throw new IllegalArgumentException("Value must be String or Integer");
        }
    }

    public abstract void setProjects(List<Project> projects);

    public abstract void fetchData();

    public List<ProjectedBranchData> getBelongings(String entity) {
        return switch (entity) {
            case "user" -> List.of(getUserBelonging());
            case "organization" -> List.of(getOrganizationBelonging());
            case "both" -> List.of(getUserBelonging(), getOrganizationBelonging());
            default -> throw new ClassCastException("Entity '" + entity + "' is not admitted as user, organization, or both");
        };
    }

    public abstract ProjectedBranchData getUserBelonging();
    public abstract ProjectedBranchData getOrganizationBelonging();
    private void fetchOrganizedBranchedProjects() {}
}