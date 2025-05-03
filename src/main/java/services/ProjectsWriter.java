package services;

import model.projects.Project;
import records.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProjectsWriter {
    private final JsonApiClient apiClient;
    private final String userEmail;

    public ProjectsWriter(String userEmail) {
        this.apiClient = new JsonApiClient();
        this.userEmail = userEmail;
    }

    public CompletableFuture<Project> createProject(Project project) {
        ApiRequest<Project> request = apiClient.buildWritingRequest(
                Project.class, ApiClient.HttpMethod.POST, userEmail, project,
                "user", "prjcts", "new"
        );
        return executeAndValidate(request);
    }

    public CompletableFuture<MeasuredGoal> addMeasuredGoal(UUID projectId, MeasuredGoal goal) {
        ApiRequest<MeasuredGoal> request = apiClient.buildWritingRequest(
                MeasuredGoal.class,
                ApiClient.HttpMethod.POST,
                userEmail,
                goal,
                "projects", projectId.toString(), "measured-goals"
        );
        return executeAndValidate(request);
    }

    public CompletableFuture<Failure> addFailure(UUID goalId, Failure failure) {
        ApiRequest<Failure> request = apiClient.buildWritingRequest(
                Failure.class,
                ApiClient.HttpMethod.POST,
                userEmail,
                failure,
                "measured-goals", goalId.toString(), "failures"
        );
        return executeAndValidate(request);
    }

    private <T> CompletableFuture<T> executeAndValidate(ApiRequest<T> request) {
        return apiClient.execute(request)
            .thenApply(response -> {
                if (!response.isSuccess()) {
                    throw new ApiException(
                            response.statusCode(),
                            request.path(),
                            "API request failed with status: " + response.statusCode()
                    );
                }
                return response.body();
            });
    }
}