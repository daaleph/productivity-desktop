package home.records;

import static data.Abbreviations.getAbbreviation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public record Priority(
        int id,
        String descriptionEn,
        String descriptionEs
) {
    @JsonCreator
    public static Priority fromJson(JsonNode node) {
        return new Priority(
                node.get("id").asInt(),
                node.get(getAbbreviation("descriptionEn")).asText(),
                node.get(getAbbreviation("descriptionEs")).asText()
        );
    }
}