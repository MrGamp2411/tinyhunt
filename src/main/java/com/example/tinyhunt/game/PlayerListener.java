package com.example.tinyhunt.game;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles runtime events affecting the TinyHunt game.
 */
public final class PlayerListener implements Listener {

    private final GameManager gameManager;

    public PlayerListener(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (gameManager.getState() != GameState.RUNNING) {
            return;
        }
        if (!gameManager.isHunter(damager)) {
            return;
        }
        if (!gameManager.isParticipant(victim)) {
            return;
        }
        if (gameManager.isHunter(victim)) {
            return;
        }
        event.setCancelled(true);
        gameManager.handleHunterHit(damager, victim);
    }
}
