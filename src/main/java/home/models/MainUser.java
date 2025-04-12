package home.models;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;

import home.models.branchs.Branch;
import home.models.branchs.UserBranch;
import home.models.organizations.UserOrganization;
import home.models.projects.CoreProject;
import home.models.projects.Enumerations;
import home.models.projects.Project;
import home.models.projects.ProjectsFetcher;
import home.records.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import static data.Abbreviations.getAbbreviation;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MainUser {
    private static MainUser instance;

    protected int age;
    protected String completeName, preferredName, email;
    protected Map<Integer, UserBranch> branches = new HashMap<>();
    protected Map<Integer, Priority> priorities = new HashMap<>();
    protected Map<UUID, CoreProject> coreProjects = new HashMap<>();
    protected Map<UUID, Project> favoriteProjects = new HashMap<>();
    protected Map<Integer, UserOrganization> organizations = new HashMap<>();

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
        fetchData();
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
        }
        return instance;
    }

    private void fetchData() {
        this.fetchPersonalData();
        this.fetchCoreProjects();
        this.fetchOrganizations();
        this.fetchFavoriteProjects();
        this.fetchBranches();
    }

    private void fetchPersonalData() {
        String user = getAbbreviation("user");
        String email = getAbbreviation("email");
        HttpClient client = HttpClient.newHttpClient();
        try {
            String apiUrl = String.format("http://localhost:4000/api/%s/?%s=%s", user, email, this.email);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                home.records.User userData = home.records.User.fromJson(root.get(0));
                this.completeName = userData.completeName();
                this.preferredName = userData.preferredName();
                this.age = userData.age();
                this.email = userData.email();
                this.priorities = userData
                        .priorities()
                        .stream()
                        .collect(Collectors.toMap(
                                Priority::id,
                                p -> new Priority(new Triplet<>(p.id(), p.descriptionEn(), p.descriptionEs()))
                        ));
            } else {
                System.err.println("Error fetching personal data. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchCoreProjects() {
        ProjectsFetcher.configure(
                new ProjectsFetcher.Config(
                        this.email,
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                )
        );
        ProjectsFetcher fetcher = ProjectsFetcher.getInstance();
        Set<Entities> entities = EnumSet.of(Entities.MAIN_USER);
        Set<Enumerations> projectsFilter = EnumSet.of(Enumerations.CORE);
        fetcher.fetch(projectsFilter, entities);
        this.coreProjects = fetcher.getAllCoreProjectsByMainUser();
    }

    private void fetchFavoriteProjects() {
        String favoriteS = getAbbreviation("favorite");
        String user = getAbbreviation("user");
        String email = getAbbreviation("email");
        String projects = getAbbreviation("projects");

        HttpClient client = HttpClient.newHttpClient();
        try {
            String apiUrl = String.format("http://localhost:4000/api/%s/%s/%s?%s=%s", user, projects, favoriteS, email, this.email);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode projectsArray = mapper.readTree(response.body());
                Map<UUID, Project> favoriteProjectsMap = new HashMap<>();
                for (JsonNode projectNode : projectsArray) {
                    int type = projectNode.get("type").asInt();
                    String name = projectNode.get("name").asText();
                    boolean favorite = projectNode.get("favorite").asBoolean();
                    UUID uuid = UUID.fromString(projectNode.get("uuid").asText());
                    List<UUID> parentProjects = projectNode.has("parentProjects") && !projectNode.get("parentProjects").isNull()
                            ? StreamSupport.stream(
                                    projectNode.get("parentProjects").spliterator(), false)
                            .map(JsonNode::asText)
                            .map(UUID::fromString)
                            .collect(Collectors.toList())
                            : Collections.emptyList();
                    Project project = new Project(uuid);
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
                            projectPriorities.add(this.getPriority(index));
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
                    List<Tuple<UUID,Triplet<Integer, String, Double>>> underlyingCategories = List.of();
                    Project.ProjectData data = new Project.ProjectData(
                            name,
                            type,
                            favorite,
                            dateToStart,
                            projectPriorities,
                            measuredGoals,
                            necessaryTime,
                            underlyingCategories,
                            parentProjects
                    );
                    project.setData(data);
                    favoriteProjectsMap.put(uuid, project);
                }
                this.favoriteProjects = favoriteProjectsMap;
            } else {
                System.err.println("Error fetching core projects. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchOrganizations() {
        String userAbbr = getAbbreviation("user");
        String emailAbbr = getAbbreviation("email");
        String organizationsAbbr = getAbbreviation("organizations");
        HttpClient client = HttpClient.newHttpClient();
        try {
            String apiUrl = String.format("http://localhost:4000/api/%s/%s?%s=%s",
                    userAbbr, organizationsAbbr, emailAbbr, this.email);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());
                Map<Integer, UserOrganization> orgsMap = new HashMap<>();

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
                            JsonNode branchData = branchEntry.getValue();
                            int branchId = branchData.get("id").asInt();
                            String branchName = branchData.get("name").asText();

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchBranches() {
        String user = getAbbreviation("user");
        String email = getAbbreviation("email");
        String branchesS = getAbbreviation("branches");

        HttpClient client = HttpClient.newHttpClient();
        try {
            String apiUrl = String.format("http://localhost:4000/api/%s/%s/?%s=%s", user, branchesS, email, this.email);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.body());

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
        } catch (Exception e) {
            e.printStackTrace();
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
        Project.ProjectData projectData = new Project.ProjectData(
                projectNode.get("name").asText(),
                projectNode.get("type").asInt(),
                projectNode.get("favorite").asBoolean(),
                ZonedDateTime.parse(projectNode.get("dateToStart").asText()),
                priorities,
                measuredGoals,
                completion,
                underlyingCategories,
                parentProjects
        );
        Project project = new Project(projectUuid);
        project.setData(projectData);
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
        if (goalsNode != null && goalsNode.isArray()) {
            goalsNode.forEach(goalNode -> {
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
        }
        return goals;
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

    @Override
    public String toString() {
        return String.format("Name: %s, Email: %s", completeName, email);
    }

}
