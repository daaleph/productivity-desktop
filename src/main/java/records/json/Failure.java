package records.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import records.secrets.JsonProps;

public record Failure(
        @JsonProperty(JsonProps.REASON) String reason,
        @JsonProperty(JsonProps.SOLUTION) String solution,
        @JsonProperty(JsonProps.DESCRIPTION) String description
) {
    public static Failure fromJson(JsonNode node) {
        return new Failure(
                node.get(JsonProps.REASON).asText(),
                node.get(JsonProps.SOLUTION).asText(),
                node.get(JsonProps.DESCRIPTION).asText()
        );
    }
}