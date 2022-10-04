package dev.mouradski.sgbnftbot.model;

public enum Network {
    
    FLARE(14),
    SONGBIRD(19);

    Integer value;

    Network(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return this.value;
    }
}
