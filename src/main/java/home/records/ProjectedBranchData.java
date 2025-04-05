package home.records;

import home.models.projects.Project;

import java.util.List;

public record ProjectedBranchData(Tuple<Object, List<Project>> tuple) {
    public Object identification() {
        return tuple.first();
    }

    public List<Project> projects() {
        return tuple.second();
    }
}