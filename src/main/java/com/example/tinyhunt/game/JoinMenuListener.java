package com.example.tinyhunt.game;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles click events within the TinyHunt join menu.
 */
public final class JoinMenuListener implements Listener {

    private final GameManager gameManager;

    public JoinMenuListener(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!gameManager.getJoinMenu().isMenu(event.getView())) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() != event.getSlot()) {
            return;
        }
        if (!gameManager.getJoinMenu().isActionSlot(event.getSlot())) {
            return;
        }
        player.closeInventory();
        gameManager.getJoinMenu().handleAction(player);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (gameManager.getJoinMenu().isMenu(event.getView())) {
            event.setCancelled(true);
        }
    }
}
