package com.example.tinyhunt.game;

import com.example.tinyhunt.model.ArenaDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Keeps participants within the configured arena bounds during a match.
 */
public final class ArenaBoundaryListener implements Listener {

    private static final long WARNING_COOLDOWN_MS = 2000L;

    private final GameManager gameManager;
    private final Map<UUID, Long> lastWarnings = new HashMap<>();

    public ArenaBoundaryListener(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) {
            return;
        }
        Player player = event.getPlayer();
        if (!gameManager.isParticipant(player) || gameManager.getState() != GameState.RUNNING) {
            return;
        }
        Optional<ArenaDefinition> optionalArena = gameManager.getActiveArena();
        if (optionalArena.isEmpty()) {
            return;
        }
        ArenaDefinition arena = optionalArena.get();
        if (!arena.getArea().isComplete()) {
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (arena.getArea().contains(to)) {
            return;
        }
        Location corrected = arena.getArea().clamp(to);
        event.setTo(corrected);
        long now = System.currentTimeMillis();
        long last = lastWarnings.getOrDefault(player.getUniqueId(), 0L);
        if (now - last >= WARNING_COOLDOWN_MS) {
            lastWarnings.put(player.getUniqueId(), now);
            player.sendMessage(gameManager.getPlugin().getMessage("messages.arena-boundary-hit"));
        }
    }
}
