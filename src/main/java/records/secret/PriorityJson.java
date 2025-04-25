package records.secret;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PriorityJson(
        @JsonProperty("id") int id,
        @JsonProperty("dscrptn_en") String descriptionEn,
        @JsonProperty("dscrptn_es") String descriptionEs
) {}