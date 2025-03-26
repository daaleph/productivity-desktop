package home.models;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import home.records.Priority;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import static data.Abbreviations.getAbbreviation;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private void fetchData() {
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
                        .map(p -> new Priority(p.id(), p.descriptionEn(), p.descriptionEs()))
                        .collect(Collectors.toList());
            } else {
                System.err.println("Error fetching data. Status code: " + response.statusCode());
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
