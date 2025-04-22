// home.records.User
package home.records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import records.Priority;
import records.Triplet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        JsonNode newNode = node.get(0);
        return new User(
                newNode.get(getAbbreviation("completeName")).asText(),
                newNode.get(getAbbreviation("preferredName")).asText(),
                newNode.get(getAbbreviation("age")).asInt(),
                parsePriorities(newNode.get(getAbbreviation("priorities"))),
                newNode.get(getAbbreviation("email")).asText()
        );
    }

    private static List<Priority> parsePriorities(JsonNode node) {
        if (node == null || !node.isArray()) return new ArrayList<>();
        return StreamSupport.stream(node.spliterator(), false)
                .map(priorityNode -> new Priority(
                        new Triplet<>(
                                priorityNode.get("id").asInt(),
                                priorityNode.get(getAbbreviation("descriptionEn")).asText(),
                                priorityNode.get(getAbbreviation("descriptionEs")).asText()
                        )
                ))
                .collect(Collectors.toList());
    }
}