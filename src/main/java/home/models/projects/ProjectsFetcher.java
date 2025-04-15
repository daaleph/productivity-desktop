package home.models.projects;

import home.records.*;

import home.Entity;
import home.HomeFetcher;
import home.models.Entities;
import home.models.MainUser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.ZonedDateTime;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static data.Abbreviations.getAbbreviation;

public class ProjectsFetcher extends HomeFetcher<ProjectsFetcher.Config> {
    String core = getAbbreviation("core");
    String userS = getAbbreviation("user");
    String emailS = getAbbreviation("email");
    String projects = getAbbreviation("projects");
    String favoriteS = getAbbreviation("favorite");

    private static Config config;
    private static volatile ProjectsFetcher instance;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Map<String, List<Project>> usersProjects = new ConcurrentHashMap<>();
    private final Map<String, List<Project>> usersFavoriteProjects = new ConcurrentHashMap<>();
    private final Map<String, List<CoreProject>> usersCoreProjects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingFetches = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(ProjectsFetcher.class.getName());

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
            fetchCoresMainUser();
            return;
        }

        if (projectTypes.contains(Enumerations.FAVORITE) && filterEntities.contains(Entities.MAIN_USER)) {
            fetchFavoritesMainUser();
            return;
        }

        if (projectTypes.contains(Enumerations.ALL) && filterEntities.contains(Entities.MAIN_USER)) {
            fetchAllMainUser();
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
    }

    private void fetchCoresMainUser() {
        pendingFetches.computeIfAbsent(config.mainEmail, this::fetchCoresByUser);
    }

    private void fetchFavoritesMainUser() {
        pendingFetches.computeIfAbsent(config.mainEmail, this::fetchFavoritesByUser);
    }

    private void fetchAllMainUser() {
        pendingFetches.computeIfAbsent(config.mainEmail, this::fetchAllByUser);
    }

    private CompletableFuture<Void> fetchCoresByUser(String email) {
        String apiUrl = String.format("http://localhost:4000/api/%s/%s/%s?%s=%s",
                userS, projects, core, emailS, email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processResponse(response, Enumerations.CORE))
                .thenAccept(projects -> usersCoreProjects.put(email, safeCastToList(projects, CoreProject.class)))
                .whenComplete((result, ex) -> {
                    if (ex != null) System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    pendingFetches.remove(email);
                });
    }

    private CompletableFuture<Void> fetchFavoritesByUser(String email) {
        String apiUrl = String.format("http://localhost:4000/api/%s/%s/%s?%s=%s",
                userS, projects, favoriteS, emailS, email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processResponse(response, Enumerations.FAVORITE))
                .thenAccept(projects -> usersFavoriteProjects.put(email, safeCastToList(projects, Project.class)))
                .whenComplete((result, ex) -> {
                    if (ex != null) System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    pendingFetches.remove(email);
                });
    }

    private CompletableFuture<Void> fetchAllByUser(String email) {
        String apiUrl = String.format("http://localhost:4000/api/%s/%s?%s=%s",
                userS, projects, emailS, email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> processResponse(response, Enumerations.ALL))
                .thenAccept(projects -> {
                    System.out.print("RESPONSE: ");
                    System.out.println(projects);
                    usersProjects.put(email, safeCastToList(projects, Project.class));
                })
                .whenComplete((result, ex) -> {
                    if (ex != null) System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    pendingFetches.remove(email);
                });
    }

    private List<? extends CoreProject> processResponse(HttpResponse<String> response, Enumerations type) {
        if (response.statusCode() != 200) {
            System.err.println("HTTP error: " + response.statusCode());
            return Collections.emptyList();
        }
        MainUser mainUser = MainUser.getInstance(config.mainEmail);
        Map<Integer, Priority> priorities = mainUser.getPriorities();
        try {
            switch (type) {
                case Enumerations.CORE -> {
                    JsonNode projectsArray = mapper.readTree(response.body());
                    List<CoreProject> coreProjects = new ArrayList<>();
                    for (JsonNode projectNode : projectsArray) {
                        CoreProject project = parseCore(projectNode, priorities);
                        coreProjects.add(project);
                    }
                    return coreProjects;
                }
                case Enumerations.ALL, Enumerations.FAVORITE -> {
                    JsonNode projectsArray = mapper.readTree(response.body());
                    List<Project> projects = new ArrayList<>();
                    for (JsonNode projectNode : projectsArray) {
                        Project project = parse(projectNode, priorities);
                        projects.add(project);
                    }
                    return projects;
                }
            }
        } catch (Exception e) {
            throw new CompletionException("Failed to parse core projects", e);
        }
        return Collections.emptyList();
    }

    private CoreProject parseCore(JsonNode projectNode, Map<Integer, Priority> priorities) {

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
        EssentialInfo essential = this.parseEssentialData(projectNode);

        // Parse priorities
        List<Priority> projectPriorities = new ArrayList<>();
        JsonNode prioritiesNode = projectNode.get("priorities");
        if (prioritiesNode != null && prioritiesNode.isArray()) {
            logger.log(Level.FINE, "Found {0} priorities", prioritiesNode.size());
            for (JsonNode pNode : prioritiesNode) {
                try {
                    int index = pNode.asInt();
                    Priority priority = priorities.get(index);
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
        JsonNode completionNode = projectNode.get("completion");
        try {
            // Parse completion data
            MeasuredSet<Integer> necessaryTime = this.parseCompletionTime(completionNode);
            CoreProject.CoreProjectInfo info = new CoreProject.CoreProjectInfo(
                    essential, projectPriorities, measuredGoals, necessaryTime, List.of()
            );
            coreProject.setInfo(info);
            logger.log(Level.FINE, "Successfully created and set project data");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create project data", e);
            throw e;
        }

        return coreProject;
    }

    private Project parse(JsonNode projectNode, Map<Integer, Priority> prioritiesMap) {

        UUID uuid;
        try {
            uuid = UUID.fromString(projectNode.get("uuid").asText());
            logger.log(Level.FINE, "Successfully parsed UUID: " + uuid);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse UUID from: " + projectNode.get("uuid"), e);
            throw e;
        }
        Project project = new Project(uuid);

        // Parse basic project data
        EssentialInfo essential = this.parseEssentialData(projectNode);

        // Parse priorities
        List<Priority> prioritiesList = parsePriorities(projectNode.get("priorities"));

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
        JsonNode completionNode = projectNode.get("completion");
        try {
            // Parse completion data
            MeasuredSet<Integer> necessaryTime = this.parseCompletionTime(completionNode);
            CoreProject.CoreProjectInfo info = new CoreProject.CoreProjectInfo(
                    essential, prioritiesList, measuredGoals, necessaryTime, List.of()
            );
            project.setInfo(info);
            logger.log(Level.FINE, "Successfully created and set project data");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create project data", e);
            throw e;
        }

        return project;
    }

    private EssentialInfo parseEssentialData(JsonNode project) {
        int type = project.get("type").asInt();
        String name = project.get("name").asText();
        boolean favorite = project.get("favorite").asBoolean();
        ZonedDateTime dateToStart = ZonedDateTime.parse(project.get("dateToStart").asText());

        return new EssentialInfo(name, type, favorite, dateToStart);
    }

    private List<Priority> parsePriorities(JsonNode prioritiesNode) {
        List<Priority> priorities = new ArrayList<>();
        if (prioritiesNode == null || !prioritiesNode.isArray()) {
            logger.log(Level.FINE, "No priorities found or priorities node is not an array");
            return priorities;
        }

        logger.log(Level.FINE, "Found {0} priorities", prioritiesNode.size());
        for (JsonNode pNode : prioritiesNode) {
            try {
                int index = pNode.asInt();
                Priority priority = MainUser.getInstance(config.mainEmail).getPriority(index);
                if (priority == null) {
                    logger.log(Level.WARNING, "Priority not found for index: " + index);
                } else {
                    priorities.add(priority);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to parse priority node: " + pNode, e);
            }
        }
        return priorities;
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

    private MeasuredSet<Integer> parseCompletionTime(JsonNode completionNode) {
        try {
            Map<String, Integer> completionMap = Map.of(
                    "days", completionNode.get("days").asInt(),
                    "weeks", completionNode.get("weeks").asInt(),
                    "months", completionNode.get("months").asInt(),
                    "years", completionNode.get("years").asInt()
            );
            MeasuredSet<Integer> necessaryTime = new MeasuredSet<>(completionMap, Integer.class);
            logger.log(Level.FINE, "Successfully parsed completion data");
            return necessaryTime;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create project data", e);
            throw e;
        }
    }

    public Map<UUID, CoreProject> getCoresOfMainUser() {
        String email = config.mainEmail;
        CompletableFuture<?> fetchOperation = pendingFetches.get(email);

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

    public Map<UUID, Project> getFavoritesOfMainUser() {
        String email = config.mainEmail;
        CompletableFuture<?> fetchOperation = pendingFetches.get(email);

        if (fetchOperation != null) fetchOperation.join();

        return usersFavoriteProjects.getOrDefault(email, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        Project::getUuid,
                        Function.identity()
                ));
    }

    public Map<UUID, Project> getAllOfMainUser() {
        String email = config.mainEmail;
        CompletableFuture<?> fetchOperation = pendingFetches.get(email);

        if (fetchOperation != null) fetchOperation.join();

        return usersProjects.getOrDefault(email, Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(
                        Project::getUuid,
                        Function.identity()
                ));
    }

    private <T extends CoreProject> List<T> safeCastToList(List<? extends CoreProject> list, Class<T> clazz) {
        List<T> result = new ArrayList<>(list.size());
        for (CoreProject item : list) {
            if (clazz.isInstance(item)) {
                result.add(clazz.cast(item));
            } else {
                throw new ClassCastException("List contains elements that are not instances of " + clazz.getName());
            }
        }
        return result;
    }
}