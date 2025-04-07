package home.models.projects;

import home.records.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class Project extends CoreProject {
    private List<UUID> parentProjects;

    public Project(UUID uuid) {
        super(uuid);
    }

    public void setData(ProjectData data) {
        super.setData(data);
        this.parentProjects = data.parentProjects;
    }

    public List<UUID> getParentProjects() {
        return parentProjects;
    }

    public static class ProjectData extends CoreProjectData {
        private final List<UUID> parentProjects;

        public ProjectData(
                String name,
                int type,
                boolean favorite,
                ZonedDateTime dateToStart,
                List<Priority> priorities,
                List<MeasuredGoal> measuredGoals,
                MeasuredSet<Integer> necessaryTime,
                List<Tuple<UUID, Triplet<Integer, String, Double>>> underlyingCategories,
                List<UUID> parentProjects
        ) {
            super(
                    name,
                    type,
                    favorite,
                    dateToStart,
                    priorities,
                    measuredGoals,
                    necessaryTime,
                    underlyingCategories
            );
            this.parentProjects = parentProjects;
        }
    }
}