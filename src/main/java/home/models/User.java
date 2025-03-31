package home.models;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import home.records.Failure;
import home.records.MeasuredGoal;
import home.records.Priority;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import static data.Abbreviations.getAbbreviation;
import com.fasterxml.jackson.databind.ObjectMapper;
import home.types.Triplet;

public class User {
    private static User instance;

    protected int age;
    protected List<Branch> branches;
    protected List<Priority> priorities;
    protected List<CoreProject> coreProjects;
    protected List<Organization> organizations;
    protected Map<String, String> abbreviations;
    protected String completeName, preferredName, email;

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

    public List<Priority> getPriorities() {
        return priorities;
    }

    public List<CoreProject> getCoreProjects() {
        return coreProjects;
    }

    private void fetchData() {
        this.fetchPersonalData();
        this.fetchCoreProjects();
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
            System.out.println(response.statusCode());
            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                home.records.User userData = home.records.User.fromJson(root.get(0));
                this.completeName = userData.completeName();
                this.preferredName = userData.preferredName();
                this.age = userData.age();
                this.email = userData.email();
                this.priorities = userData.priorities().stream()
                        .map(p -> new Priority(new Triplet<>(p.id(), p.descriptionEn(), p.descriptionEs())))
                        .toList();
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
                List<CoreProject> coreProjectsList = new ArrayList<>();
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
                    MeasuredSet<Integer> necessaryTime = MeasuredSet.create(Integer.class, completionMap);
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
                        MeasuredSet<Double> real = MeasuredSet.create(Double.class,
                                Map.of("goal", realGoal, "advance", realAdvance));
                        int discreteGoal = goalNode.get("discreteGoal").asInt();
                        int discreteAdvance = goalNode.get("discreteAdvance").asInt();
                        MeasuredSet<Integer> discrete = MeasuredSet.create(Integer.class,
                                Map.of("goal", discreteGoal, "advance", discreteAdvance));
                        List<Failure> failures = new ArrayList<>();
                        JsonNode failuresNode = goalNode.get("failures");
                        for (JsonNode failureNode : failuresNode) {
                            System.out.print("Failure:");
                            System.out.println(failureNode);
                            failures.add(Failure.fromJson(failureNode));
                        }
                        boolean finished = goalNode.get("finished").asBoolean();
                        measuredGoals.add(new MeasuredGoal(order, item, weight, real, discrete, finished, failures));
                    }
                    List<Triplet<Integer, String, Double>> underlyingCategories = List.of();
                    coreProject.setData(
                            name,
                            type,
                            favorite,
                            dateToStart,
                            projectPriorities,
                            measuredGoals,
                            necessaryTime,
                            underlyingCategories
                    );
                    coreProjectsList.add(coreProject);
                }
                this.coreProjects = coreProjectsList;
            } else {
                System.err.println("Error fetching core projects. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extractJsonValue(String json, String key) {
        String keyPattern = "\"" + key + "\":";
        int startIndex = json.indexOf(keyPattern) + keyPattern.length();
        if (startIndex < keyPattern.length()) return "";
        if (json.charAt(startIndex) == '"') {
            startIndex++;
            int endIndex = json.indexOf("\"", startIndex);
            return json.substring(startIndex, endIndex);
        } else {
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = json.indexOf("}", startIndex);
            return json.substring(startIndex, endIndex).trim();
        }
    }

    @Override
    public String toString() {
        return String.format("Name: %s, Email: %s", completeName, email);
    }

}
