package records.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import records.secrets.JsonProps;
import java.util.List;

public record MeasuredGoal(
        @JsonProperty(JsonProps.ORDER) short order,
        @JsonProperty(JsonProps.ITEM) String item,
        @JsonProperty(JsonProps.WEIGHT) float weight,
        @JsonIgnore List<Failure> failures
) {
    // Add JSON serialization/deserialization logic if needed
}