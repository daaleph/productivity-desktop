package home.models.projects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import home.Entity;
import home.HomeFetcher;
import home.models.Entities;
import home.models.MainUser;
import home.records.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static data.Abbreviations.getAbbreviation;

public class ProjectsFetcher extends HomeFetcher<ProjectsFetcher.Config> {
    private static volatile ProjectsFetcher instance;
    private volatile boolean isDataLoaded = false;
    private Map<String, List<CoreProject>> usersCoreProjects = new HashMap<String, List<CoreProject>>();
    private Map<String, List<Project>> usersFavoriteProjects = new HashMap<String, List<Project>>();
    private Map<String, List<Project>> usersProjects = new HashMap<String, List<Project>>();
    private static Config config;

    private MainUser mainUser;
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
        // Validate inputs
        Objects.requireNonNull(projectTypes, "Project types cannot be null");
        Objects.requireNonNull(filterEntities, "Filter types cannot be null");
        if (projectTypes.isEmpty() || filterEntities.isEmpty()) {
            throw new IllegalArgumentException("Project and filter types must not be empty");
        }

        // Use the config values from the singleton
        List<String> emails = config.emails;
        List<Integer> branches = config.branches;
        List<Integer> organizations = config.organizations;

        if (projectTypes.contains(Enumerations.CORE) && filterEntities.contains(Entities.MAIN_USER)) {
            fetchAllCoreProjectsByMainUser();
            return;
        }

        // Apply filters
        if (filterEntities.contains(Entities.EMAILS)) {
            System.out.println("Filtering by emails: " + emails);
            // Add email-based filtering logic
        }
        if (filterEntities.contains(Entities.ORGANIZATIONS)) {
            System.out.println("Filtering by organizations: " + organizations);
            // Add organization-based filtering logic
        }
        if (filterEntities.contains(Entities.BRANCHES)) {
            System.out.println("Filtering by branches: " + branches);
            // Add branch-based filtering logic
        }

        // Fetch projects based on type
        if (projectTypes.contains(Enumerations.CORE)) {
            System.out.println("Fetching core projects");
            // Logic for core projects
        }
        if (projectTypes.contains(Enumerations.ALL)) {
            System.out.println("Fetching all projects");
            // Logic for all projects
        }
        if (projectTypes.contains(Enumerations.FAVORITE)) {
            System.out.println("Fetching favorite projects");
            // Logic for favorite projects
        }
    }

    private void fetchCoreByUsers() {
        List<String> emails = config.emails;
    }

    private synchronized void fetchAllCoreProjectsByUser(String email) {
        HttpClient client = HttpClient.newHttpClient();
        try {
            isDataLoaded = false;
            String apiUrl = String.format("http://localhost:4000/api/%s/%s/%s?%s=%s", userS, projects, core, emailS, email);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode projectsArray = mapper.readTree(response.body());
                for (JsonNode projectNode : projectsArray) {
                    UUID uuid = UUID.fromString(projectNode.get("uuid").asText());
                    CoreProject coreProject = new CoreProject(uuid);
                    String name = projectNode.get("name").asText();
                    int type = projectNode.get("type").asInt();
                    boolean favorite = projectNode.get("favorite").asBoolean();
                    ZonedDateTime dateToStart = ZonedDateTime.parse(projectNode.get("dateToStart").asText());
                    JsonNode completionNode = projectNode.get("completion");
                    Map<String, Integer> completionMap = Map.of(
                            "days", completionNode.get("days").asInt(),
                            "weeks", completionNode.get("weeks").asInt(),
                            "months", completionNode.get("months").asInt(),
                            "years", completionNode.get("years").asInt()
                    );
                    MeasuredSet<Integer> necessaryTime = new MeasuredSet<>(completionMap, Integer.class);
                    List<Priority> projectPriorities = new ArrayList<>();
                    JsonNode prioritiesNode = projectNode.get("priorities");
                    if (prioritiesNode.isArray()) {
                        for (JsonNode pNode : prioritiesNode) {
                            int index = pNode.asInt();
                            projectPriorities.add(MainUser.getInstance(config.mainEmail).getPriority(index));
                        }
                    }
                    List<MeasuredGoal> measuredGoals = new ArrayList<>();
                    JsonNode goalsNode = projectNode.get("measuredGoals");
                    for (JsonNode goalNode : goalsNode) {
                        int order = goalNode.get("order").asInt();
                        String item = goalNode.get("item").asText();
                        double weight = goalNode.get("weight").asDouble();
                        double realGoal = goalNode.get("realGoal").asDouble();
                        double realAdvance = goalNode.get("realAdvance").asDouble();
                        int discreteGoal = goalNode.get("discreteGoal").asInt();
                        int discreteAdvance = goalNode.get("discreteAdvance").asInt();

                        MeasuredSet<Double> real = new MeasuredSet<>(
                                Map.of("goal", realGoal, "advance", realAdvance),
                                Double.class
                        );
                        MeasuredSet<Integer> discrete = new MeasuredSet<>(
                                Map.of("goal", discreteGoal, "advance", discreteAdvance),
                                Integer.class
                        );

                        List<Failure> failures = new ArrayList<>();
                        JsonNode failuresNode = goalNode.get("failures");
                        for (JsonNode failureNode : failuresNode) {
                            failures.add(Failure.fromJson(failureNode));
                        }
                        boolean finished = goalNode.get("finished").asBoolean();
                        measuredGoals.add(new MeasuredGoal(order, item, weight, real, discrete, finished, failures));
                    }
                    List<Tuple<UUID, Triplet<Integer, String, Double>>> underlyingCategories = List.of();
                    CoreProject.CoreProjectData data = new CoreProject.CoreProjectData(
                            name,
                            type,
                            favorite,
                            dateToStart,
                            projectPriorities,
                            measuredGoals,
                            necessaryTime,
                            underlyingCategories
                    );
                    coreProject.setData(data);
                    System.out.print("CORE PROJECT: ");
                    System.out.println(coreProject);
                    List<CoreProject> coreProjects = this.usersCoreProjects.get(email);
                    List<CoreProject> newCoreProjects = new ArrayList<>(coreProjects);
                    newCoreProjects.add(coreProject);
                    this.usersCoreProjects.put(email, newCoreProjects);
                    isDataLoaded = true;
                }
            } else {
                System.err.println("Error fetching core projects for user. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchAllCoreProjectsByMainUser() {
        String mainEmail = config.mainEmail;
        this.fetchAllCoreProjectsByUser(mainEmail);
    }

    public Map<UUID, CoreProject> getAllCoreProjectsByMainUser() {
        Map<UUID, CoreProject> coreProjects = this.usersCoreProjects.get(config.mainEmail)
                .stream()
                .collect(Collectors.toMap(
                        CoreProject::getUuid,
                        Function.identity()
                ));
        System.out.print("CORE PROJECTS: ");
        System.out.println(coreProjects);
        return coreProjects;
    }
}