package com.example.tinyhunt.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Configuration-backed description of a named arena: boundary and spawn points.
 */
public final class ArenaDefinition {

    private final String name;
    private final ConfiguredArea area;
    private final List<Location> spawns;

    public ArenaDefinition(String name) {
        this(name, new ConfiguredArea(), new ArrayList<>());
    }

    public ArenaDefinition(String name, ConfiguredArea area, List<Location> spawns) {
        this.name = Objects.requireNonNull(name, "name");
        this.area = area != null ? area : new ConfiguredArea();
        this.spawns = new ArrayList<>(Objects.requireNonNull(spawns, "spawns"));
    }

    public String getName() {
        return name;
    }

    public ConfiguredArea getArea() {
        return area;
    }

    public List<Location> getSpawns() {
        return Collections.unmodifiableList(spawns);
    }

    public void addSpawn(Location location) {
        spawns.add(Objects.requireNonNull(location, "location"));
    }

    public void clearSpawns() {
        spawns.clear();
    }

    public void replaceSpawns(List<Location> locations) {
        spawns.clear();
        if (locations != null) {
            spawns.addAll(locations);
        }
    }

    public void save(ConfigurationSection section) {
        Objects.requireNonNull(section, "section");
        ConfigurationSection areaSection = getOrCreate(section, "area");
        area.save(areaSection);
        ConfigurationSection spawnsSection = getOrCreate(section, "spawns");
        ConfigLocationUtil.writeLocationList(spawnsSection, spawns);
    }

    public static ArenaDefinition load(String name, ConfigurationSection section) {
        if (section == null) {
            return new ArenaDefinition(name);
        }
        ConfiguredArea area = ConfiguredArea.load(section.getConfigurationSection("area"));
        List<Location> spawns = ConfigLocationUtil.readLocationList(section.getConfigurationSection("spawns"));
        return new ArenaDefinition(name, area, spawns);
    }

    private ConfigurationSection getOrCreate(ConfigurationSection parent, String path) {
        ConfigurationSection child = parent.getConfigurationSection(path);
        if (child == null) {
            child = parent.createSection(path);
        }
        return child;
    }
}
