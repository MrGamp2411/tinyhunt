package com.kjaza.tinymmo.skill;

import java.util.Objects;
import java.util.function.BiConsumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Skill {
    public interface SkillAction extends BiConsumer<Player, Plugin> {
        @Override
        void accept(Player player, Plugin plugin);
    }

    private final String id;
    private final Material mat;
    private final int cooldownSec;
    private final ResourceType resource;
    private final int cost;
    private final SkillAction action;

    public Skill(String id, Material mat, int cooldownSec, ResourceType resource, int cost,
            SkillAction action) {
        this.id = Objects.requireNonNull(id, "id");
        this.mat = Objects.requireNonNull(mat, "mat");
        this.cooldownSec = cooldownSec;
        this.resource = Objects.requireNonNull(resource, "resource");
        this.cost = cost;
        this.action = action;
    }

    public String id() {
        return id;
    }

    public Material mat() {
        return mat;
    }

    public int cooldownSec() {
        return cooldownSec;
    }

    public ResourceType resource() {
        return resource;
    }

    public int cost() {
        return cost;
    }

    public void execute(Player player, Plugin plugin) {
        if (action != null) {
            action.accept(player, plugin);
        }
    }
}
