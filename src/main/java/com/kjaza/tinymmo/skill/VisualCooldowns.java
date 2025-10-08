package com.kjaza.tinymmo.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class VisualCooldowns {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private final Plugin plugin;
    private final Map<UUID, Map<String, Long>> track = new HashMap<>();

    public VisualCooldowns(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player p, Skill skill, int seconds) {
        p.setCooldown(skill.mat(), seconds * 20);
        track.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>())
             .put(skill.id(), System.currentTimeMillis() + seconds * 1000L);
    }

    public void tickActionbar() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Map<String, Long> m = track.get(p.getUniqueId());
            if (m == null || m.isEmpty()) {
                continue;
            }
            String label = null;
            int rem = Integer.MAX_VALUE;
            for (Map.Entry<String, Long> entry : m.entrySet()) {
                int r = (int) Math.ceil((entry.getValue() - now) / 1000.0);
                if (r > 0 && r < rem) {
                    rem = r;
                    label = entry.getKey();
                }
            }
            if (label != null) {
                Component message = LEGACY_SERIALIZER.deserialize(ChatColor.GRAY + "Cooldown: "
                        + ChatColor.WHITE + label + ChatColor.DARK_GRAY + " " + rem + "s");
                p.sendActionBar(message);
            }
            m.entrySet().removeIf(en -> en.getValue() <= now);
        }
    }
}
