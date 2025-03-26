// home.records.User
package home.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import static data.Abbreviations.getAbbreviation;

public record User(
        String completeName,
        String preferredName,
        int age,
        List<Priority> priorities,
        String email
) {
    @JsonCreator
    public static User fromJson(JsonNode node) {
        return new User(
            node.get(getAbbreviation("completeName")).asText(),
            node.get(getAbbreviation("preferredName")).asText(),
            node.get(getAbbreviation("age")).asInt(),
            parsePriorities(node.get(getAbbreviation("priorities"))),
            node.get(getAbbreviation("email")).asText()
        );
    }

    private static List<Integer> parseIntegerList(JsonNode node) {
        List<Integer> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode element : node) {
            list.add(element.asInt());
        }
        return list;
    }

    private static List<String> parseStringList(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        for (JsonNode element : node) {
            list.add(element.asText());
        }
        return list;
    }

    private static List<Priority> parsePriorities(JsonNode node) {
        List<Priority> priorities = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return priorities;
        }
        for (JsonNode priorityNode : node) {
            int id = priorityNode.get("id").asInt();
            String descriptionEn = priorityNode.get(getAbbreviation("descriptionEn")).asText();
            String descriptionEs = priorityNode.get(getAbbreviation("descriptionEs")).asText();
            priorities.add(new Priority(id, descriptionEn, descriptionEs));
        }
        return priorities;
    }
}