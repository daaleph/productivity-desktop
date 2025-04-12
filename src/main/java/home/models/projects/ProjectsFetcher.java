package home.models.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import home.Entity;
import home.HomeFetcher;
import home.models.Entities;
import home.models.MainUser;
import home.records.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static data.Abbreviations.getAbbreviation;

public class ProjectsFetcher extends HomeFetcher<ProjectsFetcher.Config> {
    private static Config config;
    private volatile boolean isDataLoaded = false;
    private static volatile ProjectsFetcher instance;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Map<String, List<CoreProject>> usersCoreProjects = new ConcurrentHashMap<>();
    private final Map<String, List<Project>> usersProjects = new ConcurrentHashMap<>();
    private final Map<String, List<Project>> usersFavoriteProjects = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> pendingFetches = new ConcurrentHashMap<>();

    String core = getAbbreviation("core");
    String userS = getAbbreviation("user");
    String emailS = getAbbreviation("email");
    String projects = getAbbreviation("projects");

    // Configuration class for this subclass
    public static class Config {
        private final String mainEmail;
        private final List<String> emails;
        private final List<Integer> organizations;
        private final List<Integer> branches;

        public Config(
                String mainEmail,
                List<String> emails,
                List<Integer> organizations,
                List<Integer> branches
        ) {
            this.emails = new ArrayList<>(emails);
            if (!this.emails.contains(mainEmail)) this.emails.add(mainEmail);
            this.branches = branches;
            this.mainEmail = mainEmail;
            this.organizations = organizations;
        }
    }

    // Configure the singleton before first use
    public static void configure(Config config) {
        if (instance != null) {
            throw new IllegalStateException("Singleton already initialized");
        }
        ProjectsFetcher.config = config;
    }

    // Singleton accessor with double-checked locking
    public static ProjectsFetcher getInstance() {
        if (instance == null) {
            synchronized (ProjectsFetcher.class) {
                if (instance == null) {
                    if (config == null) {
                        throw new IllegalStateException("Call configure() first");
                    }
                    instance = new ProjectsFetcher();
                }
            }
        }
        return instance;
    }

    // Private constructor uses the subclass-specific config
    private ProjectsFetcher() {
        super(config);
    }

    // Initialize Entity using the Config
    @Override
    protected Entity createEntity(Config config) {
        return new Entity.Builder()
                .users(config.emails)
                .branches(config.branches)
                .organizations(config.organizations)
                .build();
    }

    public void fetch(Set<Enumerations> projectTypes, Set<Entities> filterEntities) {
        Objects.requireNonNull(projectTypes, "Project types cannot be null");
        Objects.requireNonNull(filterEntities, "Filter types cannot be null");
        if (projectTypes.isEmpty() || filterEntities.isEmpty()) {
            throw new IllegalArgumentException("Project and filter types must not be empty");
        }

        if (projectTypes.contains(Enumerations.CORE) && filterEntities.contains(Entities.MAIN_USER)) {
            fetchAllCoresMainUser();
            return;
        }

        // Existing filtering logic remains unchanged
        if (filterEntities.contains(Entities.EMAILS)) {
            System.out.println("Filtering by emails: " + config.emails);
        }
        if (filterEntities.contains(Entities.ORGANIZATIONS)) {
            System.out.println("Filtering by organizations: " + config.organizations);
        }
        if (filterEntities.contains(Entities.BRANCHES)) {
            System.out.println("Filtering by branches: " + config.branches);
        }

        if (projectTypes.contains(Enumerations.ALL)) {
            System.out.println("Fetching all projects");
        }
        if (projectTypes.contains(Enumerations.FAVORITE)) {
            System.out.println("Fetching favorite projects");
        }
    }

    private void fetchAllCoresMainUser() {
        String email = config.mainEmail;
        pendingFetches.computeIfAbsent(email, this::fetchAllCoresByUser);
    }

    private CompletableFuture<Void> fetchAllCoresByUser(String email) {
        String apiUrl = String.format("http://localhost:4000/api/%s/%s/%s?%s=%s",
                userS, projects, core, emailS, email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::processCoreProjectsResponse)
                .thenAccept(projects -> usersCoreProjects.put(email, projects))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    }
                    pendingFetches.remove(email);
                });
    }

    private List<CoreProject> processCoreProjectsResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            System.err.println("HTTP error: " + response.statusCode());
            return Collections.emptyList();
        }

        try {
            JsonNode projectsArray = mapper.readTree(response.body());
            List<CoreProject> projects = new ArrayList<>();
            for (JsonNode projectNode : projectsArray) {
                CoreProject project = parseCoreProject(projectNode);
                projects.add(project);
            }
            return projects;
        } catch (Exception e) {
            throw new CompletionException("Failed to parse core projects", e);
        }
    }

    private static final Logger logger = Logger.getLogger(ProjectsFetcher.class.getName());

    private CoreProject parseCoreProject(JsonNode projectNode) {
        try {
            logger.log(Level.FINE, "Starting to parse core project");

            // Parse UUID
            UUID uuid;
            try {
                uuid = UUID.fromString(projectNode.get("uuid").asText());
                logger.log(Level.FINE, "Successfully parsed UUID: " + uuid);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to parse UUID from: " + projectNode.get("uuid"), e);
                throw e;
            }
            CoreProject coreProject = new CoreProject(uuid);

            // Parse basic project data
            String name = projectNode.get("name").asText();
            int type = projectNode.get("type").asInt();
            boolean favorite = projectNode.get("favorite").asBoolean();
            ZonedDateTime dateToStart = ZonedDateTime.parse(projectNode.get("dateToStart").asText());
            logger.log(Level.FINE, "Parsed basic project data - Name: {0}, Type: {1}, Favorite: {2}, Start: {3}",
                    new Object[]{name, type, favorite, dateToStart});

            // Parse completion data
            JsonNode completionNode = projectNode.get("completion");
            try {
                Map<String, Integer> completionMap = Map.of(
                        "days", completionNode.get("days").asInt(),
                        "weeks", completionNode.get("weeks").asInt(),
                        "months", completionNode.get("months").asInt(),
                        "years", completionNode.get("years").asInt()
                );
                MeasuredSet<Integer> necessaryTime = new MeasuredSet<>(completionMap, Integer.class);
                logger.log(Level.FINE, "Successfully parsed completion data");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to parse completion data", e);
                throw e;
            }

            // Parse priorities
            List<Priority> projectPriorities = new ArrayList<>();
            JsonNode prioritiesNode = projectNode.get("priorities");
            if (prioritiesNode != null && prioritiesNode.isArray()) {
                logger.log(Level.FINE, "Found {0} priorities", prioritiesNode.size());
                for (JsonNode pNode : prioritiesNode) {
                    try {
                        int index = pNode.asInt();
                        Priority priority = MainUser.getInstance(config.mainEmail).getPriority(index);
                        if (priority == null) {
                            logger.log(Level.WARNING, "Priority not found for index: " + index);
                        } else {
                            projectPriorities.add(priority);
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to parse priority node: " + pNode, e);
                    }
                }
            } else {
                logger.log(Level.FINE, "No priorities found or priorities node is not an array");
            }

            // Parse measured goals
            List<MeasuredGoal> measuredGoals;
            try {
                measuredGoals = parseMeasuredGoals(projectNode.get("measuredGoals"));
                logger.log(Level.FINE, "Parsed {0} measured goals", measuredGoals.size());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to parse measured goals", e);
                throw e;
            }

            // Create and set project data
            try {
                Map<String, Integer> completionMap = Map.of(
                        "days", completionNode.get("days").asInt(),
                        "weeks", completionNode.get("weeks").asInt(),
                        "months", completionNode.get("months").asInt(),
                        "years", completionNode.get("years").asInt()
                );
                MeasuredSet<Integer> necessaryTime = new MeasuredSet<>(completionMap, Integer.class);
                CoreProject.CoreProjectData data = new CoreProject.CoreProjectData(
                        name, type, favorite, dateToStart,
                        projectPriorities, measuredGoals, necessaryTime, List.of()
                );
                coreProject.setData(data);
                logger.log(Level.FINE, "Successfully created and set project data");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to create project data", e);
                throw e;
            }

            return coreProject;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Critical error parsing core project", e);
            throw e;
        }
    }

    private List<MeasuredGoal> parseMeasuredGoals(JsonNode goalsNode) {
        List<MeasuredGoal> goals = new ArrayList<>();
        if (goalsNode == null || !goalsNode.isArray()) {
            return goals;
        }

        for (JsonNode goalNode : goalsNode) {
            int order = goalNode.get("order").asInt();
            String item = goalNode.get("item").asText();
            double weight = goalNode.get("weight").asDouble();

            MeasuredSet<Double> real = new MeasuredSet<>(
                    Map.of(
                            "goal", goalNode.get("realGoal").asDouble(),
                            "advance", goalNode.get("realAdvance").asDouble()
                    ),
                    Double.class
            );

            MeasuredSet<Integer> discrete = new MeasuredSet<>(
                    Map.of(
                            "goal", goalNode.get("discreteGoal").asInt(),
                            "advance", goalNode.get("discreteAdvance").asInt()
                    ),
                    Integer.class
            );

            List<Failure> failures = parseFailures(goalNode.get("failures"));
            boolean finished = goalNode.get("finished").asBoolean();

            goals.add(new MeasuredGoal(
                    order, item, weight, real, discrete, finished, failures
            ));
        }
        return goals;
    }

    private List<Failure> parseFailures(JsonNode failuresNode) {
        List<Failure> failures = new ArrayList<>();
        if (failuresNode == null || !failuresNode.isArray()) {
            return failures;
        }

        for (JsonNode failureNode : failuresNode) {
            failures.add(Failure.fromJson(failureNode));
        }
        return failures;
    }

    public Map<UUID, CoreProject> getAllCoresOfMainUser() {
        String email = config.mainEmail;
        CompletableFuture<Void> fetchOperation = pendingFetches.get(email);

        // Wait for completion if fetch is in progress
        if (fetchOperation != null) {
            fetchOperation.join();
        }

        return usersCoreProjects.getOrDefault(email, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        CoreProject::getUuid,
                        Function.identity()
                ));
    }
}