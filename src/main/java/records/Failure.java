package records;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

import static data.Abbreviations.getAbbreviation;

public record Failure(Triplet<String, String, String> triplet) {

    public String reason() {
        return triplet.first();
    }
    public String solution() {
        return triplet.second();
    }
    public String description() {
        return triplet.third();
    }

    @JsonCreator
    public static Failure fromJson(JsonNode node) {
        return new Failure(
            new Triplet<>(
                    node.get(getAbbreviation("reason")).asText(),
                    node.get(getAbbreviation("solution")).asText(),
                    node.get(getAbbreviation("description")).asText()
            )
        );
    }

    public void logEntity() {
        String measuredGoalStructure = String.format(
                """
                Failure {
                    description: %s,
                    reason: "%s",
                    solution: %s,
                }""",
                description(), reason(), solution()
        );
        System.out.println(measuredGoalStructure);
    }
}