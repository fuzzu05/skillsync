package com.skillsync.enums;

public enum Proficiency {
    BEGINNER(1),
    INTERMEDIATE(2),
    ADVANCED(3),
    EXPERT(4);

    private final int weight;

    Proficiency(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
