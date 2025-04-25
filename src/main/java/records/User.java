// records.User
package records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import enumerations.Languages;
import records.secrets.PriorityJson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static data.Abbreviations.getAbbreviation;

public record User(
        String completeName,
        String preferredName,
        int age,
        List<PriorityJson> priorities,
        String email,
        Languages language
) {
    @JsonCreator
    public static User fromJson(JsonNode node) {
        JsonNode newNode = node.get(0);
        Languages preferredLanguage;
        int intPreferredLanguage = newNode.get(getAbbreviation("preferredLanguage")).asInt();
        boolean spanish = intPreferredLanguage == 0;
        if (spanish) {
            preferredLanguage = Languages.SPANISH;
        } else {
            preferredLanguage = Languages.ENGLISH;
        }
        return new User(
                newNode.get(getAbbreviation("completeName")).asText(),
                newNode.get(getAbbreviation("preferredName")).asText(),
                newNode.get(getAbbreviation("age")).asInt(),
                parsePriorities(newNode.get(getAbbreviation("priorities")), preferredLanguage),
                newNode.get(getAbbreviation("email")).asText(),
                preferredLanguage
        );
    }

    private static List<PriorityJson> parsePriorities(JsonNode node, Languages language) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        return StreamSupport.stream(node.spliterator(), false)
                .map(priorityNode -> new PriorityJson(
                                priorityNode.get("id").asInt(),
                                priorityNode.get(getAbbreviation("descriptionEn")).asText(),
                                priorityNode.get(getAbbreviation("descriptionEs")).asText()
                ))
                .collect(Collectors.toList());
    }
}