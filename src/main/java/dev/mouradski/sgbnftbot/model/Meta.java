package dev.mouradski.sgbnftbot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Meta {
    private String name;
    private String description;
    private String image;
    private String dna;
    private Integer edition;
}
