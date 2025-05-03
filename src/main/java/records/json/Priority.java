package records.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import static records.secrets.JsonProps.*;

public record Priority(
        @JsonProperty(ID) int id,
        @JsonProperty(DESCRIPTION_EN) String descriptionEn,
        @JsonProperty(DESCRIPTION_ES) String descriptionEs
) {}