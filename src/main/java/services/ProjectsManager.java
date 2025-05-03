package services;

import home.MainUser;
import model.projects.Project;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ProjectsManager {
//    private final ProjectsFetcher fetcher;
    private final ProjectsWriter writer;

    public ProjectsManager(MainUser mainUser) {
//        this.fetcher = ProjectsFetcher.getInstance();
        this.writer = new ProjectsWriter(mainUser.getEmail());
    }

    public CompletableFuture<Project> createProject(Project project) {
        return writer
                .createProject(project)
                .thenApply(created -> {
                    System.out.printf("STATE OF CREATING PROJECT: %s%n", created.toString().toUpperCase(Locale.ROOT));
                    return created;
                })
                .exceptionally(ex -> {
                    System.err.println("Error creating project: " + ex.getMessage());
                    return null;
                });
    }

//    public CompletableFuture<MeasuredGoal> addMeasuredGoal(UUID projectId, MeasuredGoal goal) {
//        return writer.addMeasuredGoal(projectId, goal)
//                .thenApply(created -> created);
//    }
}