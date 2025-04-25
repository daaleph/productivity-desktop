package model.projects;

import enumerations.Languages;

import home.Entity;
import home.HomeFetcher;
import model.Entities;
import home.MainUser;

import java.net.http.HttpClient;

import java.time.ZonedDateTime;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import records.*;

import services.JsonApiClient;

import static data.Abbreviations.getAbbreviation;

public class ProjectsFetcher extends HomeFetcher<ProjectsFetcher.Config> {
    private final String CORE = getAbbreviation("core");
    private final String USER = getAbbreviation("user");
    private final String EMAIL = getAbbreviation("email");
    private final String PROJECTS = getAbbreviation("projects");
    private final String FAVORITE = getAbbreviation("favorite");
    private final String ORG = getAbbreviation("organizations");

    private static Config config;
    private static volatile ProjectsFetcher instance;

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonApiClient apiClient = new JsonApiClient();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Map<String, List<Project>> usersProjects = new ConcurrentHashMap<>();
    private final Map<String, List<Project>> usersFavoriteProjects = new ConcurrentHashMap<>();
    private final Map<String, List<CoreProject>> usersCoreProjects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingFetches = new ConcurrentHashMap<>();

    private static final Logger logger = Logger.getLogger(ProjectsFetcher.class.getName());

    // Configuration class for this subclass
    public static class Config {
        private final Languages language;
        private final String mainEmail;
        private final List<String> emails;
        private final List<Integer> organizations;
        private final List<Integer> branches;

        public Config(
                String mainEmail,
                Languages language,
                List<String> emails,
                List<Integer> organizations,
                List<Integer> branches
        ) {
            this.emails = new ArrayList<>(emails);
            if (!this.emails.contains(mainEmail)) this.emails.add(mainEmail);
            this.language = language;
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
        ApiRequest<JsonNode> request = apiClient.buildUserApiRequest(JsonNode.class, email, USER, PROJECTS, CORE);
        return apiClient.execute(request)
                .thenApply(apiResponse -> processAPIResponse(apiResponse, Enumerations.CORE))
                .thenAccept(projects -> usersCoreProjects.put(email, safeCastToList(projects, CoreProject.class)))
                .whenComplete((result, ex) -> {
                    if (ex != null) System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    pendingFetches.remove(email);
                });
    }

    private CompletableFuture<Void> fetchFavoritesByUser(String email) {
        ApiRequest<JsonNode> request = apiClient.buildUserApiRequest(JsonNode.class, email, USER, PROJECTS, FAVORITE);
        return apiClient.execute(request)
                .thenApply(apiResponse -> processAPIResponse(apiResponse, Enumerations.FAVORITE))
                .thenAccept(projects -> usersFavoriteProjects.put(email, safeCastToList(projects, Project.class)))
                .whenComplete((result, ex) -> {
                    if (ex != null) System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    pendingFetches.remove(email);
                });
    }

    private CompletableFuture<Void> fetchAllByUser(String email) {
        ApiRequest<JsonNode> request = apiClient.buildUserApiRequest(JsonNode.class, email, USER, PROJECTS);
        return apiClient.execute(request)
                .thenApply(apiResponse -> processAPIResponse(apiResponse, Enumerations.ALL))
                .thenAccept(projects -> usersProjects.put(email, safeCastToList(projects, Project.class)))
                .whenComplete((result, ex) -> {
                    if (ex != null) System.err.println("Async fetch failed for " + email + ": " + ex.getMessage());
                    pendingFetches.remove(email);
                });
    }

    private List<? extends CoreProject> processAPIResponse(ApiResponse<JsonNode> apiResponse, Enumerations type) {
        if (apiResponse.statusCode() != 200) {
            System.err.println("HTTP error: " + apiResponse.statusCode());
            return Collections.emptyList();
        }
        MainUser mainUser = MainUser.getInstance(config.mainEmail);
        Map<Integer, Priority> priorities = mainUser.getPriorities();
        try {
            switch (type) {
                case CORE -> {
                    JsonNode projectsArray = apiResponse.body();
                    List<CoreProject> coreProjects = new ArrayList<>();
                    for (JsonNode projectNode : projectsArray) {
                        CoreProject project = parseCore(projectNode, priorities);
                        coreProjects.add(project);
                    }
                    return coreProjects;
                }
                case ALL, FAVORITE -> {
                    JsonNode projectsArray = apiResponse.body();
                    List<Project> projects = new ArrayList<>();
                    for (JsonNode projectNode : projectsArray) {
                        Project project = parse(projectNode, priorities);
                        projects.add(project);
                    }
                    return projects;
                }
                default -> {
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            throw new CompletionException("Failed to parse projects", e);
        }
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

        EssentialInfo essential = this.parseEssentialData(projectNode);

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


        List<MeasuredGoal> measuredGoals;
        try {
            measuredGoals = parseMeasuredGoals(projectNode.get("measuredGoals"));
            logger.log(Level.FINE, "Parsed {0} measured goals", measuredGoals.size());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse measured goals", e);
            throw e;
        }

        JsonNode completionNode = projectNode.get("completion");
        try {
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

        // Parsing
        EssentialInfo essential = this.parseEssentialData(projectNode);
        List<Priority> prioritiesList = parsePriorities(projectNode.get("priorities"));
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
        if (goalsNode == null || !goalsNode.isArray()) {
            return new ArrayList<>();
        }
        return StreamSupport.stream(goalsNode.spliterator(), false)
                .map(this::parseGoalNode)
                .collect(Collectors.toList());
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

    private MeasuredGoal parseGoalNode(JsonNode goalNode) {
        int order = goalNode.get("order").asInt();
        String item = goalNode.get("item").asText();
        double weight = goalNode.get("weight").asDouble();
        boolean finished = goalNode.get("finished").asBoolean();
        JsonNode failures = goalNode.get("failures");

        MeasuredSet<Double> real = createMeasuredGoal(
                goalNode, "realGoal", "realAdvance", JsonNode::asDouble, Double.class);
        MeasuredSet<Integer> discrete = createMeasuredGoal(
                goalNode, "discreteGoal", "discreteAdvance", JsonNode::asInt, Integer.class);

        return new MeasuredGoal(order, item, weight, real, discrete, finished, parseFailures(failures));
    }

    private <T> MeasuredSet<T> createMeasuredGoal(
            JsonNode goalNode, String goalField, String advanceField, Function<JsonNode, T> extractor, Class<T> type
    ) {
        return new MeasuredSet<>(
                Map.of(
                        "goal", extractor.apply(goalNode.get(goalField)),
                        "advance", extractor.apply(goalNode.get(advanceField))
                ),
                type
        );
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

        if (fetchOperation != null) fetchOperation.join(); // Wait for completion if fetch is in progress

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

        if (fetchOperation != null) fetchOperation.join(); // Wait for completion if fetch is in progress

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