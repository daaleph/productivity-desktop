package home.models.branchs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import home.models.projects.Project;
import home.records.ProjectedBranchData;

public class Branch {
    private final int id;
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
    private Map<String, ProjectedBranchData> projects;

    public ProjectedBranch(int id, Object value) {
        super(id);
        addBelonging(value);
    }

    public void addBelonging(Object value) {
        if (!(value instanceof UUID || value instanceof Integer)) {
            throw new IllegalArgumentException("Value must be UUID or Integer");
        }
        fetchData(value);
    }

    private void fetchData(Object value) {
        if (value instanceof UUID) fetchUserBranchedProjects((UUID) value);
        assert value instanceof Integer;
        fetchOrganizedBranchedProjects((Integer) value);
    }

    public List<ProjectedBranchData> getBelongings(String entity) {
        return switch (entity) {
            case "user" -> List.of(getUserBelonging());
            case "organization" -> List.of(getOrganizationBelonging());
            case "both" -> List.of(getUserBelonging(), getOrganizationBelonging());
            default -> throw new ClassCastException("Entity '" + entity + "' is not admitted as user, organization, or both");
        };
    }

    public ProjectedBranchData getUserBelonging() {
        ProjectedBranchData belonging = this.projects.get("user");
        if (belonging == null) return null;
        if (belonging.identification() instanceof UUID) return belonging;
        throw new ClassCastException("Identification of '" + belonging + "' is not a UUID");
    }

    public ProjectedBranchData getOrganizationBelonging() {
        ProjectedBranchData belonging = this.projects.get("organization");
        if (belonging == null) return null;
        if (belonging.identification() instanceof Integer) return belonging;
        throw new ClassCastException("Identification of '" + belonging + "' is not an Integer");
    }

    private void fetchUserBranchedProjects(UUID uuid) {

    }

    private void fetchOrganizedBranchedProjects(Integer id) {

    }
}