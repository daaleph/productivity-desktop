package home.records;

import static data.Abbreviations.getAbbreviation;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import home.types.Triplet;

public record Priority(Triplet<Integer, String, String> triplet) {

    public int id() {
        return triplet.first();
    }

    public String descriptionEn() {
        return triplet.second();
    }

    public String descriptionEs() {
        return triplet.third();
    }

    @JsonCreator
    public static Priority fromJson(JsonNode node) {
        return new Priority(
            new Triplet<>(
                node.get("id").asInt(),
                node.get(getAbbreviation("descriptionEn")).asText(),
                node.get(getAbbreviation("descriptionEs")).asText()
            )
        );
    }

}