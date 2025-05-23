package home;

import com.fasterxml.jackson.databind.JsonNode;

import enumerations.Languages;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import model.Entities;
import model.branchs.Branch;
import model.branchs.UserBranch;
import model.organizations.UserOrganization;
import model.projects.*;

import records.*;
import records.secrets.*;
import services.ApiException;
import services.JsonApiClient;

import static data.Abbreviations.getAbbreviation;

public class MainUser {
    private static MainUser instance;
    private final JsonApiClient apiClient = new JsonApiClient();

    private final String ID = getAbbreviation("id");
    private final String USER = getAbbreviation("user");
    private final String ORG = getAbbreviation("organizations");
    private final String BRANCHES = getAbbreviation("branches");
    private final String TABLE = getAbbreviation("table");
    private final String WEIGHT = getAbbreviation("weight");

    protected int age;
    protected Languages language;
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

    private void fetchCoreProjects() {
        fetchProjectData(Enumerations.CORE, ProjectsFetcher::getCoresOfMainUser, cores -> this.coreProjects = cores);
    }

    private void fetchFavoriteProjects() {
        fetchProjectData(Enumerations.FAVORITE, ProjectsFetcher::getFavoritesOfMainUser, favorites -> this.favoriteProjects = favorites);
    }

    private void fetchProjects() {
        fetchProjectData(Enumerations.ALL, ProjectsFetcher::getAllOfMainUser, projects -> this.projects = projects);
    }

    private void fetchPersonalInfo() {
        try {
            ApiRequest<User> request = apiClient.buildUserApiRequest(User.class, this.email, USER);
            User userInfo = apiClient.executeApiRequest(request);
            this.completeName = userInfo.completeName();
            this.preferredName = userInfo.preferredName();
            this.language = userInfo.language();
            this.age = userInfo.age();
            this.email = userInfo.email();
            this.priorities = userInfo
                    .priorities()
                    .stream()
                    .collect(Collectors.toMap(
                            PriorityJson::id,
                            p -> new Priority(
                                    new Triplet<>(p.id(), p.descriptionEn(), p.descriptionEs()),
                                    this.language
                            )
                    ));

        } catch (InterruptedException e) {
            throw new ApiException("Failed to fetch personal info", e);
        }
    }

    private void fetchOrganizations() {
        try {
            ApiRequest<JsonNode> request = apiClient.buildUserApiRequest(JsonNode.class, this.email, USER, ORG);

            JsonNode rootNode = apiClient.executeApiRequest(request);
            Map<Integer, UserOrganization> orgsMap = new HashMap<>();

            rootNode.fields().forEachRemaining(orgEntry -> {
                JsonNode orgNode = orgEntry.getValue();
                int orgId = orgNode.get("id").asInt();
                String name = orgNode.get("name").asText();
                String email = orgNode.get("email").asText();
                JsonNode branchesNode = orgNode.get("branches");

                UserOrganization userOrg = new UserOrganization(orgId, email);
                userOrg.setName(name);

                Map<Integer, Branch> branches = new HashMap<>();

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
        } catch (InterruptedException e) {
            throw new ApiException("Failed to fetch organizations", e);
        }
    }

    private void fetchBranches() {
        try {
            ApiRequest<JsonNode> request = apiClient.buildUserApiRequest(JsonNode.class, this.email, USER, BRANCHES);
            JsonNode rootNode = apiClient.executeApiRequest(request);

            rootNode.fields().forEachRemaining(entry -> {
                int branchId = Integer.parseInt(entry.getKey());
                JsonNode branchNode = entry.getValue();

                if (branchNode.get("name") == null) {
                    System.out.print("BRANCH NAMES: ");
                    System.out.println(branchNode);
                }
                String branchName = branchNode.get("name").asText();
                JsonNode projectsNode = branchNode.get("projects");

                List<Project> projects = new ArrayList<>();

                if (projectsNode != null && !projectsNode.isEmpty()) {
                    projectsNode.fields().forEachRemaining(projectEntry -> {
                        String projectUuidStr = projectEntry.getKey();
                        JsonNode projectNode = projectEntry.getValue();
                        Project project = parseProject(projectNode, projectUuidStr);
                        projects.add(project);
                    });
                }

                UserBranch userBranch = new UserBranch(branchId, this.email);
                userBranch.setName(branchName);
                userBranch.setProjects(projects);
                branches.put(branchId, userBranch);
            });
        } catch (InterruptedException e) {
            throw new ApiException("Failed to fetch branches", e);
        }
    }

    private Project parseProject(JsonNode projectNode, String projectUuidStr) {
        UUID projectUuid = UUID.fromString(projectUuidStr);

        List<Priority> priorities = parsePriorities(projectNode.get("priorities"));
        List<MeasuredGoal> measuredGoals = parseMeasuredGoals(projectNode.get("measuredGoals"));
        MeasuredSet<Integer> completion = parseMeasuredSet(projectNode.get("completion"));
        List<UUID> parentProjects = parseParentProjects(projectNode.get("parentProjects"));

        List<Tuple<UUID, Triplet<Integer, String, Double>>> underlyingCategories =
                parseUnderlyingCategories(projectNode.get("underlyingCategories"));

        EssentialInfo essentialInfo = new EssentialInfo(
                projectNode.get("name").asText(),
                projectNode.get("type").asInt(),
                projectNode.get("favorite").asBoolean(),
                ZonedDateTime.parse(projectNode.get("dateToStart").asText())
        );

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
                    priorities.add(Priority.fromInt(priorityNode, this.language));
                });
            } else {
                prioritiesNode.forEach(priorityNode -> {
                    priorities.add(Priority.fromJson(priorityNode, this.language));
                });
            }
        }
        return priorities;
    }

    private List<MeasuredGoal> parseMeasuredGoals(JsonNode goalsNode) {
        if (goalsNode == null || !goalsNode.isArray()) return new ArrayList<>();

        List<MeasuredGoal> goals = new ArrayList<>();
        goalsNode.forEach(goalNode -> {
            goals.add(new MeasuredGoal(
                    goalNode.get("order").asInt(),
                    goalNode.get("item").asText(),
                    goalNode.get("weight").asDouble(),
                    createMeasuredGoal(goalNode, "real", JsonNode::asDouble, Double.class),
                    createMeasuredGoal(goalNode, "discrete", JsonNode::asInt, Integer.class),
                    goalNode.get("finished").asBoolean(),
                    parseFailures(goalNode.get("failures"))
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
        if (categoriesNode == null || categoriesNode.get("uuid").isNull() || !categoriesNode.has("uuid") || !categoriesNode.has("priorities")) return new ArrayList<>();

        List<Tuple<UUID, Triplet<Integer, String, Double>>> categories = new ArrayList<>();
        UUID categoryUuid = UUID.fromString(categoriesNode.get("uuid").asText());
        JsonNode prioritiesNode = categoriesNode.get("priorities");

        if (prioritiesNode.isArray()) {
            for (JsonNode priorityNode : prioritiesNode) {
                if (priorityNode.isEmpty()) continue;
                int id = priorityNode.get(ID).asInt();
                String table = priorityNode.get(TABLE).asText();
                double weight = priorityNode.get(WEIGHT).asDouble();
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

    public boolean isConvertibleToIntegerList(JsonNode arrayNode) {
        for (JsonNode element : arrayNode) {
            if (!element.isInt() && !element.isLong() && !element.canConvertToInt()) return false;
        }
        return true;
    }

    private <T> void fetchProjectData(
            Enumerations type,
            Function<ProjectsFetcher, Map<UUID, T>> resultExtractor,
            Consumer<Map<UUID, T>> resultSetter
    ) {
        ProjectsFetcher.configure(projectsFetcher());
        ProjectsFetcher fetcher = ProjectsFetcher.getInstance();
        fetcher.fetch(EnumSet.of(type), EnumSet.of(Entities.MAIN_USER));
        resultSetter.accept(resultExtractor.apply(fetcher));
    }

    private ProjectsFetcher.Config projectsFetcher() {
        return new ProjectsFetcher.Config(
                this.email,
                this.language,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Email: %s", completeName, email);
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

}
