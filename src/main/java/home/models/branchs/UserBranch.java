package home.models.branchs;

import home.models.MainUser;
import home.records.Tuple;
import home.models.projects.Project;
import home.records.ProjectedBranchData;

import java.util.List;

public class UserBranch extends ProjectedBranch {
    private String email;

    public UserBranch(int id, String email) {
        super(id, email);
    }

    @Override
    public void addBelonging(Object value) {
        super.addBelonging(value);
        if (!(value instanceof String)) throw new IllegalArgumentException("The parameter must be an String");
        this.email = (String) value;
    }

    @Override
    public void setProjects(List<Project> projects) {
        Tuple<Object, List<Project>> tuple = new Tuple<>(this.id, projects);
        this.projects = new ProjectedBranchData(tuple);
    }

    @Override
    public void fetchData() {
        this.fetchUserBranchedProjects();
    }

    public ProjectedBranchData getUserBelonging() {
        ProjectedBranchData belonging = this.projects;
        if (belonging == null) return null;
        if (belonging.identification() instanceof Integer) return belonging;
        throw new ClassCastException("Identification of '" + belonging + "' is not a UUID");
    }

    @Override
    public ProjectedBranchData getOrganizationBelonging() {
        return null;
    }

    private void fetchUserBranchedProjects() {

    }

    public String getEmail() {
        return this.email;
    }
}