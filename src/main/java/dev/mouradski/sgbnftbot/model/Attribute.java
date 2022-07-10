package dev.mouradski.sgbnftbot.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class Attribute {
    @JsonProperty("trait_type")
    private String traitType;
    private String value;
}
