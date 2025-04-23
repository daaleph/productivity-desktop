package home.records;

import com.fasterxml.jackson.databind.JsonNode;
import enumerations.Languages;
import records.Triplet;

import static data.Abbreviations.getAbbreviation;

public record Priority(Triplet<Integer, String, String> triplet, Languages language) {

    public int id() {
        return triplet.first();
    }

    public String getName() {
        if (language == Languages.ENGLISH) return descriptionEn();
        return descriptionEs();
    }

    public String descriptionEn() {
        return triplet.second();
    }

    public String descriptionEs() {
        return triplet.third();
    }

    public static Priority fromJson(JsonNode node, Languages language) {
        return new Priority(
            new Triplet<>(
                node.get("id").asInt(),
                node.get(getAbbreviation("descriptionEn")).asText(),
                node.get(getAbbreviation("descriptionEs")).asText()
            ),
            language
        );
    }

    public static Priority fromInt(JsonNode node, Languages language) {
        return new Priority(
                new Triplet<>(
                        node.asInt(),
                        "English",
                        "Spanish"
                ),
                language
        );
    }

}