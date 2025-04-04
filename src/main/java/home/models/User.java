package home.models;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.*;

import home.models.branchs.Branch;
import home.models.organizations.UserOrganization;
import home.models.projects.CoreProject;
import home.models.projects.Project;
import home.records.Failure;
import home.records.MeasuredGoal;
import home.records.MeasuredSet;
import home.records.Priority;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import static data.Abbreviations.getAbbreviation;
import com.fasterxml.jackson.databind.ObjectMapper;
import home.types.Triplet;

public class User {
    private static User instance;

    protected int age;
    protected Map<Integer, Branch> branches;
    protected Map<Integer, Priority> priorities;
    protected Map<UUID, CoreProject> coreProjects;
    protected Map<UUID, Project> favoriteProjects;
    protected String completeName, preferredName, email;
    protected Map<Integer, UserOrganization> organizations;

    /**
     * Returns the single instance of User. On the first call, the provided
     * parameters are used to create the instance and fetch data. Subsequent calls will return
     * the already created instance, ignoring any parameters.
     *
     * @param email the user's email (used only during initialization)
     */
    private User(
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
    public static User getInstance(
        String email
    ) {
        if (instance == null) {
            instance = new User(email);
        }
        return instance;
    }

    private void fetchData() {
        this.fetchPersonalData();
        this.fetchCoreProjects();
        this.fetchUserOrganizations();
        this.fetchFavoriteProjects();
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
        String core = getAbbreviation("core");
        String user = getAbbreviation("user");
        String email = getAbbreviation("email");
        String projects = getAbbreviation("projects");

        HttpClient client = HttpClient.newHttpClient();
        try {
            String apiUrl = String.format("http://localhost:4000/api/%s/%s/%s?%s=%s", user, projects, core, email, this.email);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode projectsArray = mapper.readTree(response.body());
                Map<UUID, CoreProject> coreProjectsMap = new HashMap<>();
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
                    List<Triplet<Integer, String, Double>> underlyingCategories = List.of();
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
                    coreProjectsMap.put(uuid, coreProject);
                }
                this.coreProjects = coreProjectsMap;
            } else {
                System.err.println("Error fetching core projects. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    List<Triplet<Integer, String, Double>> underlyingCategories = List.of();
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

    private void fetchUserOrganizations() {
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

                            Branch branch = new Branch(branchId);
                            branch.setName(branchName);
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

    public UserOrganization getOrganization(Integer org) {
        return organizations.get(org);
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Email: %s", completeName, email);
    }

}
