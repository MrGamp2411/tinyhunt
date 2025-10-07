package com.example.tinyhunt.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Utility helpers for serializing locations into the plugin configuration.
 */
public final class ConfigLocationUtil {

    private ConfigLocationUtil() {
    }

    public static void writeLocation(ConfigurationSection section, Location location) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(location, "location");
        section.set("world", location.getWorld() != null ? location.getWorld().getName() : null);
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    public static Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static List<Location> readLocationList(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyList();
        }
        List<Location> locations = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            Location location = readLocation(section.getConfigurationSection(key));
            if (location != null) {
                locations.add(location);
            }
        }
        return locations;
    }

    public static void writeLocationList(ConfigurationSection section, List<Location> locations) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(locations, "locations");
        for (String key : section.getKeys(false)) {
            section.set(key, null);
        }
        for (int i = 0; i < locations.size(); i++) {
            ConfigurationSection entry = section.createSection(String.valueOf(i));
            writeLocation(entry, locations.get(i));
        }
    }
}
