package com.kjaza.tinymmo.skill;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class ResourceManager {
    private final Map<UUID, EnumMap<ResourceType, Integer>> pools = new HashMap<>();

    public boolean tryConsume(Player player, ResourceType type, int amount) {
        if (amount <= 0) {
            return true;
        }
        EnumMap<ResourceType, Integer> map = pools.get(player.getUniqueId());
        if (map == null || map.isEmpty()) {
            return true;
        }
        int current = map.getOrDefault(type, 0);
        if (current < amount) {
            return false;
        }
        map.put(type, current - amount);
        return true;
    }

    public void setResource(Player player, ResourceType type, int amount) {
        pools.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(ResourceType.class))
                .put(type, amount);
    }

    public int getResource(Player player, ResourceType type) {
        EnumMap<ResourceType, Integer> map = pools.get(player.getUniqueId());
        if (map == null) {
            return 0;
        }
        return map.getOrDefault(type, 0);
    }
}
