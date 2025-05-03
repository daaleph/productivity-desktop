package model.projects;

import com.fasterxml.jackson.annotation.*;
import records.*;
import records.secrets.JsonProps;

import java.util.List;
import java.util.UUID;

public class Project extends CoreProject {
    @JsonProperty(JsonProps.PARENT_PROJECTS)
    private List<UUID> parentProjects;

    public Project(UUID uuid) {
        super(uuid);
    }

    public void setInfo(ProjectInfo data) {
        super.setInfo(data);
        this.parentProjects = data.parentProjects;
    }

    public List<UUID> getParentProjects() {
        return parentProjects;
    }

    @Override
    public void logEntity() {
        super.logEntity();
        System.out.println("Parent projects:");
        for (UUID parentProject : getParentProjects()) {
            System.out.printf("  - Parent project: %s", parentProject);
        }
    }

    public static class ProjectInfo extends CoreProjectInfo {
        private final List<UUID> parentProjects;

        public ProjectInfo(
                EssentialInfo essential,
                List<Priority> priorities,
                List<MeasuredGoal> measuredGoals,
                MeasuredSet<Integer> necessaryTime,
                List<Tuple<UUID, Triplet<Integer, String, Double>>> underlyingCategories,
                List<UUID> parentProjects
        ) {
            super(
                    essential,
                    priorities,
                    measuredGoals,
                    necessaryTime,
                    underlyingCategories
            );
            this.parentProjects = parentProjects;
        }
    }
}