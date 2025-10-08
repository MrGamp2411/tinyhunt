package com.kjaza.tinymmo.skill;

import java.util.Arrays;

public enum ResourceType {
    MANA("mana"),
    STAMINA("stamina"),
    ENERGY("energy");

    public final String key;

    ResourceType(String key) {
        this.key = key;
    }

    public static ResourceType from(String key) {
        if (key == null || key.isEmpty()) {
            return MANA;
        }
        return Arrays.stream(values())
                .filter(type -> type.key.equalsIgnoreCase(key) || type.name().equalsIgnoreCase(key))
                .findFirst()
                .orElse(MANA);
    }
}
