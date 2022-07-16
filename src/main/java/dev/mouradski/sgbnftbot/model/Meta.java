package dev.mouradski.sgbnftbot.model;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Meta {
    private String name;
    private String description;
    private String image;
    private String dna;
    private Integer edition;
    private List<Attribute> attributes = null;
}
