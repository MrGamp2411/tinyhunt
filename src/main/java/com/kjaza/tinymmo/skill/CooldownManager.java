package com.kjaza.tinymmo.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CooldownManager {
    private final Map<UUID, Map<String, Long>> map = new HashMap<>();

    public boolean isOnCooldown(Player p, String skillId) {
        Long until = map.getOrDefault(p.getUniqueId(), Map.of()).get(skillId);
        return until != null && until > System.currentTimeMillis();
    }

    public void start(Player p, String skillId, int seconds) {
        map.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
           .put(skillId, System.currentTimeMillis() + seconds * 1000L);
    }

    public int remaining(Player p, String skillId) {
        Long until = map.getOrDefault(p.getUniqueId(), Map.of()).get(skillId);
        if (until == null) {
            return 0;
        }
        long diff = until - System.currentTimeMillis();
        return diff > 0 ? (int) Math.ceil(diff / 1000.0) : 0;
    }
}
