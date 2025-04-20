package home.models;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;

import home.models.branchs.Branch;
import home.models.branchs.UserBranch;
import home.models.organizations.UserOrganization;
import home.models.projects.*;
import home.records.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import records.ApiRequest;
import records.ApiResponse;
import services.ApiClient;
import services.ApiException;
import services.JsonApiClient;

import static data.Abbreviations.getAbbreviation;

public class MainUser {
    private static MainUser instance;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiClient apiClient = new JsonApiClient();

    protected int age;
    protected String completeName, preferredName, email;
    protected Map<Integer, UserBranch> branches = new HashMap<>();
    protected Map<Integer, Priority> priorities = new HashMap<>();
    protected Map<UUID, CoreProject> coreProjects = new HashMap<>();
    protected Map<Integer, UserOrganization> organizations = new HashMap<>();
    protected Map<UUID, Project> favoriteProjects, projects = new HashMap<>();

    /**
     * Returns the single instance of User. On the first call, the provided
     * parameters are used to create the instance and fetch data. Subsequent calls will return
     * the already created instance, ignoring any parameters.
     *
     * @param email the user's email (used only during initialization)
     */
    private MainUser(
            String email
    ) {
        this.email = email;
        fetchInfo();
    }

    /**
     * Returns the single instance of User. On the first call, the provided
     * parameters are used to create the instance. Subsequent calls will return
     * the already created instance, ignoring any parameters.
     *
     * @param email the user's email (used only during initialization)
     * @return the singleton User instance
     */
    public static MainUser getInstance(
            String email
    ) {
        if (instance == null) {
            instance = new MainUser(email);
            instance.fetchAsyncInfo();
        }
        return instance;
    }

    private void fetchInfo() {
        fetchPersonalInfo();
        fetchOrganizations();
        fetchBranches();
    }

    private void fetchAsyncInfo() {
        fetchProjects();
        fetchCoreProjects();
        fetchFavoriteProjects();
    }

    private void fetchPersonalInfo() {
        String userAbbr = getAbbreviation("user");
        String emailAbbr = getAbbreviation("email");

        try {
            ApiRequest<User> request = new ApiRequest<>(
                    "/" + userAbbr,
                    ApiClient.HttpMethod.GET,
                    Map.of(emailAbbr, this.email),
                    null,
                    User.class
            );
            ApiResponse<User> response = apiClient.execute(request).get();

            if (response.isSuccess()) {
                User userInfo = response.body();
                this.completeName = userInfo.completeName();
                this.preferredName = userInfo.preferredName();
                this.age = userInfo.age();
                this.email = userInfo.email();
                this.priorities = userInfo.priorities().stream()
                        .collect(Collectors.toMap(
                                Priority::id,
                                p -> new Priority(new Triplet<>(p.id(), p.descriptionEn(), p.descriptionEs()))
                        ));
            } else {
                System.err.println("Error fetching personal data. Status: " + response.statusCode());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ApiException("Failed to fetch personal info", e);
        }
    }

    private void fetchCoreProjects() {
        ProjectsFetcher.configure(projectsFetcher());
        ProjectsFetcher fetcher = ProjectsFetcher.getInstance();
        fetcher.fetch(EnumSet.of(Enumerations.CORE), EnumSet.of(Entities.MAIN_USER));
        this.coreProjects = fetcher.getCoresOfMainUser();
    }

    private void fetchFavoriteProjects() {
        ProjectsFetcher.configure(projectsFetcher());
        ProjectsFetcher fetcher = ProjectsFetcher.getInstance();
        fetcher.fetch(EnumSet.of(Enumerations.FAVORITE), EnumSet.of(Entities.MAIN_USER));
        this.favoriteProjects = fetcher.getFavoritesOfMainUser();
    }

    private void fetchProjects() {
        ProjectsFetcher.configure(projectsFetcher());
        ProjectsFetcher fetcher = ProjectsFetcher.getInstance();
        fetcher.fetch(EnumSet.of(Enumerations.ALL), EnumSet.of(Entities.MAIN_USER));
        this.projects = fetcher.getAllOfMainUser();
    }

    private void fetchOrganizations() {
        String userAbbr = getAbbreviation("user");
        String orgAbbr = getAbbreviation("organizations");
        String emailAbbr = getAbbreviation("email");

        try {
            ApiRequest<JsonNode> request = new ApiRequest<>(
                    "/" + userAbbr + "/" + orgAbbr,
                    ApiClient.HttpMethod.GET,
                    Map.of(emailAbbr, this.email),
                    null,
                    JsonNode.class
            );

            ApiResponse<JsonNode> response = apiClient.execute(request).get();

            if (response.isSuccess()) {
                Map<Integer, UserOrganization> orgsMap = new HashMap<>();
                JsonNode rootNode = response.body();

                rootNode.fields().forEachRemaining(orgEntry -> {
                    JsonNode orgNode = orgEntry.getValue();
                    int orgId = orgNode.get("id").asInt();
                    String name = orgNode.get("name").asText();
                    String email = orgNode.get("email").asText();

                    UserOrganization userOrg = new UserOrganization(orgId, email);
                    userOrg.setName(name);

                    Map<Integer, Branch> branches = new HashMap<>();
                    JsonNode branchesNode = orgNode.get("branches");
                    if (branchesNode != null) {
                        branchesNode.fields().forEachRemaining(branchEntry -> {
                            JsonNode branchInfo = branchEntry.getValue();
                            int branchId = branchInfo.get("id").asInt();
                            String branchName = branchInfo.get("name").asText();

                            Branch branch = new Branch(branchId, branchName);
                            branches.put(branchId, branch);
                        });
                    }
                    userOrg.setBranches(branches);

                    orgsMap.put(orgId, userOrg);
                });

                this.organizations = orgsMap;
            } else {
                System.err.println("Error fetching organizations: " + response.statusCode());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ApiException("Failed to fetch organizations", e);
        }
    }

    private void fetchBranches() {
        String userAbbr = getAbbreviation("user");
        String branchesAbbr = getAbbreviation("branches");
        String emailAbbr = getAbbreviation("email");

        try {
            ApiRequest<JsonNode> request = new ApiRequest<>(
                    "/" + userAbbr + "/" + branchesAbbr,
                    ApiClient.HttpMethod.GET,
                    Map.of(emailAbbr, this.email),
                    null,
                    JsonNode.class
            );

            ApiResponse<JsonNode> response = apiClient.execute(request).get();

            if (response.isSuccess()) {
                JsonNode rootNode = response.body();

                // Iterate over each branch in the JSON
                rootNode.fields().forEachRemaining(entry -> {
                    int branchId = Integer.parseInt(entry.getKey());
                    JsonNode branchNode = entry.getValue();

                    // Extract branch name
                    String branchName = branchNode.get("name").asText();

                    // Extract projects (if they exist)
                    JsonNode projectsNode = branchNode.get("projects");
                    List<Project> projects = new ArrayList<>();

                    if (projectsNode != null && !projectsNode.isEmpty()) {
                        projectsNode.fields().forEachRemaining(projectEntry -> {
                            String projectUuidStr = projectEntry.getKey();
                            JsonNode projectNode = projectEntry.getValue();

                            // Parse project details
                            Project project = parseProject(projectNode, projectUuidStr);
                            projects.add(project);
                        });
                    }

                    // Create UserBranch and store projects
                    UserBranch userBranch = new UserBranch(branchId, this.email);
                    userBranch.setName(branchName);
                    userBranch.setProjects(projects);
                    branches.put(branchId, userBranch);
                });
            } else {
                System.err.println("Error fetching projected branch data. Status code: " + response.statusCode());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new ApiException("Failed to fetch branches", e);
        }
    }

    private Project parseProject(JsonNode projectNode, String projectUuidStr) {
        UUID projectUuid = UUID.fromString(projectUuidStr);
        List<Priority> priorities = parsePriorities(projectNode.get("priorities"));
        List<MeasuredGoal> measuredGoals = parseMeasuredGoals(projectNode.get("measuredGoals"));
        MeasuredSet<Integer> completion = parseMeasuredSet(projectNode.get("completion"));
        List<Tuple<UUID, Triplet<Integer, String, Double>>> underlyingCategories =
                parseUnderlyingCategories(projectNode.get("underlyingCategories"));
        List<UUID> parentProjects = parseParentProjects(projectNode.get("parentProjects"));
        EssentialInfo essentialInfo = new EssentialInfo(projectNode.get("name").asText(),
                projectNode.get("type").asInt(),
                projectNode.get("favorite").asBoolean(),
                ZonedDateTime.parse(projectNode.get("dateToStart").asText()));
        Project.ProjectInfo projectInfo = new Project.ProjectInfo(
                essentialInfo,
                priorities,
                measuredGoals,
                completion,
                underlyingCategories,
                parentProjects
        );
        Project project = new Project(projectUuid);
        project.setInfo(projectInfo);
        return project;
    }

    private List<Priority> parsePriorities(JsonNode prioritiesNode) {
        boolean isConvertibleToIntegerList = this.isConvertibleToIntegerList(prioritiesNode);
        List<Priority> priorities = new ArrayList<>();
        if (prioritiesNode.isArray()) {
            if (isConvertibleToIntegerList) {
                prioritiesNode.forEach(priorityNode -> {
                    priorities.add(Priority.fromInt(priorityNode));
                });
            } else {
                prioritiesNode.forEach(priorityNode -> {
                    priorities.add(Priority.fromJson(priorityNode));
                });
            }
        }
        return priorities;
    }

    private List<MeasuredGoal> parseMeasuredGoals(JsonNode goalsNode) {
        List<MeasuredGoal> goals = new ArrayList<>();
        if (goalsNode == null || !goalsNode.isArray()) {
            return goals;
        }

        goalsNode.forEach(goalNode -> {
            MeasuredSet<Double> real = createMeasuredGoal(goalNode, "real", JsonNode::asDouble, Double.class);
            MeasuredSet<Integer> discrete = createMeasuredGoal(goalNode, "discrete", JsonNode::asInt, Integer.class);
            List<Failure> failures = parseFailures(goalNode.get("failures"));
            goals.add(new MeasuredGoal(
                    goalNode.get("order").asInt(),
                    goalNode.get("item").asText(),
                    goalNode.get("weight").asDouble(),
                    real,
                    discrete,
                    goalNode.get("finished").asBoolean(),
                    failures
            ));
        });
        return goals;
    }

    private <T> MeasuredSet<T> createMeasuredGoal(JsonNode goalNode, String prefix, Function<JsonNode, T> valueExtractor, Class<T> type) {
        return new MeasuredSet<>(
                Map.of(
                        "goal", valueExtractor.apply(goalNode.get(prefix + "Goal")),
                        "advance", valueExtractor.apply(goalNode.get(prefix + "Advance"))
                ),
                type
        );
    }

    private MeasuredSet<Integer> parseMeasuredSet(JsonNode completionNode) {
        if (completionNode == null) return null;
        Map<String, Integer> quantities = new HashMap<>();
        completionNode.fields().forEachRemaining(entry -> {
            quantities.put(entry.getKey(), entry.getValue().asInt());
        });
        return new MeasuredSet<>(quantities, Integer.class);
    }

    private List<Tuple<UUID, Triplet<Integer, String, Double>>> parseUnderlyingCategories(JsonNode categoriesNode) {
        List<Tuple<UUID, Triplet<Integer, String, Double>>> categories = new ArrayList<>();
        if (categoriesNode == null || !categoriesNode.has("priorities")) {
            return categories;
        }
        UUID categoryUuid = UUID.fromString(categoriesNode.get("uuid").asText());
        JsonNode prioritiesNode = categoriesNode.get("priorities");
        if (prioritiesNode.isArray()) {
            for (JsonNode priorityNode : prioritiesNode) {
                // Skip empty priorities (like [{}])
                if (priorityNode.isEmpty()) continue;
                // Extract priority fields
                int id = priorityNode.get("id").asInt(); // Assuming 'id' is an Integer (adjust if UUID)
                String table = priorityNode.get("tble").asText();
                double weight = priorityNode.get("wght").asDouble();
                // Create Triplet and Tuple
                Triplet<Integer, String, Double> triplet = new Triplet<>(id, table, weight);
                Tuple<UUID, Triplet<Integer, String, Double>> tuple = new Tuple<>(categoryUuid, triplet);
                categories.add(tuple);
            }
        }

        return categories;
    }

    private List<UUID> parseParentProjects(JsonNode parentProjectsNode) {
        if (parentProjectsNode == null || !parentProjectsNode.isArray()) return null;
        List<UUID> parentProjects = new ArrayList<>();
        parentProjectsNode.forEach(uuidNode -> {
            parentProjects.add(UUID.fromString(uuidNode.asText()));
        });
        return parentProjects;
    }

    private List<Failure> parseFailures(JsonNode failuresNode) {
        List<Failure> failures = new ArrayList<>();
        if (failuresNode != null && failuresNode.isArray()) {
            failuresNode.forEach(failureNode -> {
                failures.add(Failure.fromJson(failureNode));
            });
        }
        return failures;
    }

    public String getName() {
        return completeName;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public int getAge() {
        return age;
    }

    public String getEmail() {
        return email;
    }

    public Priority getPriority(Integer index) {
        return priorities.get(index);
    }

    public Map<Integer, Priority> getPriorities() {
        return priorities;
    }

    public Map<UUID, Project> getProjects() {
        return projects;
    }

    public Map<UUID, CoreProject> getCoreProjects() {
        return coreProjects;
    }

    public Map<UUID, Project> getFavoriteProjects() {
        return favoriteProjects;
    }

    public Map<Integer, UserOrganization> getOrganizations() {
        return organizations;
    }

    public Map<Integer, UserBranch> getBranches() {
        return branches;
    }

    public UserOrganization getOrganization(Integer org) {
        return organizations.get(org);
    }

    public boolean isConvertibleToIntegerList(JsonNode arrayNode) {
        for (JsonNode element : arrayNode) {
            if (!element.isInt() && !element.isLong() && !element.canConvertToInt()) {
                return false;
            }
        }
        return true;
    }

    private ProjectsFetcher.Config projectsFetcher() {
        return new ProjectsFetcher.Config(
                this.email,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Email: %s", completeName, email);
    }

}
