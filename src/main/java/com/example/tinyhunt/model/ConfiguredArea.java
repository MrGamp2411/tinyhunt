package com.example.tinyhunt.model;

import java.util.Objects;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

/**
 * Represents a cuboid area defined by two corner positions.
 */
public final class ConfiguredArea {

    private Location pos1;
    private Location pos2;

    public Optional<Location> getPos1() {
        return Optional.ofNullable(pos1);
    }

    public Optional<Location> getPos2() {
        return Optional.ofNullable(pos2);
    }

    public void setPos1(Location location) {
        this.pos1 = Objects.requireNonNull(location, "location");
    }

    public void setPos2(Location location) {
        this.pos2 = Objects.requireNonNull(location, "location");
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld());
    }

    public World getWorld() {
        if (!isComplete()) {
            throw new IllegalStateException("Area is not fully configured");
        }
        return pos1.getWorld();
    }

    public Vector getMinimum() {
        if (!isComplete()) {
            throw new IllegalStateException("Area is not fully configured");
        }
        return new Vector(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
    }

    public Vector getMaximum() {
        if (!isComplete()) {
            throw new IllegalStateException("Area is not fully configured");
        }
        return new Vector(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));
    }

    public Location getCenter() {
        if (!isComplete()) {
            throw new IllegalStateException("Area is not fully configured");
        }
        Vector min = getMinimum();
        Vector max = getMaximum();
        return new Location(getWorld(), (min.getX() + max.getX()) / 2.0, (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0);
    }

    public Location getRandomLocation() {
        if (!isComplete()) {
            throw new IllegalStateException("Area is not fully configured");
        }
        Vector min = getMinimum();
        Vector max = getMaximum();
        double x = min.getX() + Math.random() * (max.getX() - min.getX());
        double y = min.getY() + Math.random() * (max.getY() - min.getY());
        double z = min.getZ() + Math.random() * (max.getZ() - min.getZ());
        return new Location(getWorld(), x, y, z);
    }

    public void save(ConfigurationSection section) {
        Objects.requireNonNull(section, "section");
        if (!isComplete()) {
            section.set("world", null);
            section.set("pos1", null);
            section.set("pos2", null);
            return;
        }
        section.set("world", getWorld().getName());
        ConfigurationSection pos1Section = getOrCreate(section, "pos1");
        ConfigurationSection pos2Section = getOrCreate(section, "pos2");
        setLocation(pos1Section, pos1);
        setLocation(pos2Section, pos2);
    }

    public static ConfiguredArea load(ConfigurationSection section) {
        ConfiguredArea area = new ConfiguredArea();
        if (section == null) {
            return area;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return area;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return area;
        }
        Location loadedPos1 = readLocation(section.getConfigurationSection("pos1"), world);
        Location loadedPos2 = readLocation(section.getConfigurationSection("pos2"), world);
        if (loadedPos1 != null && loadedPos2 != null) {
            area.setPos1(loadedPos1);
            area.setPos2(loadedPos2);
        }
        return area;
    }

    private static void setLocation(ConfigurationSection section, Location location) {
        for (String key : section.getKeys(false)) {
            section.set(key, null);
        }
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }

    private static ConfigurationSection getOrCreate(ConfigurationSection parent, String path) {
        ConfigurationSection child = parent.getConfigurationSection(path);
        if (child == null) {
            child = parent.createSection(path);
        }
        return child;
    }

    private static Location readLocation(ConfigurationSection section, World fallbackWorld) {
        if (section == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(fallbackWorld, x, y, z, yaw, pitch);
    }
}
