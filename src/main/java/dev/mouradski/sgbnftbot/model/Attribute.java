package dev.mouradski.sgbnftbot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class Attribute {
    @JsonProperty("trait_type")
    private String traitType;
    private String value;
}
