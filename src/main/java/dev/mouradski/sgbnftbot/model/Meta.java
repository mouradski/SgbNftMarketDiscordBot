package dev.mouradski.sgbnftbot.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Meta {
    private String name;
    private String description;
    private String image;
    private String dna;
    private Integer edition;
    private List<Attribute> attributes = null;
}
