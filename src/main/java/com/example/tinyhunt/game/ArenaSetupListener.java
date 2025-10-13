package com.example.tinyhunt.game;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Captures chat prompts and wand interactions while configuring arenas.
 */
public final class ArenaSetupListener implements Listener {

    private final GameManager gameManager;

    public ArenaSetupListener(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        ArenaSetupManager setupManager = gameManager.getArenaSetupManager();
        if (!setupManager.isAwaitingArenaName(uuid)) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Bukkit.getScheduler().runTask(gameManager.getPlugin(),
                () -> setupManager.completeArenaName(player, message));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ArenaSetupManager setupManager = gameManager.getArenaSetupManager();
        if (setupManager.getEditingArena(player.getUniqueId()).isEmpty()) {
            return;
        }
        if (!setupManager.isSelectionWand(event.getItem())) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Location target = block.getLocation().toCenterLocation();
        boolean first = action == Action.LEFT_CLICK_BLOCK;
        setupManager.handleSelection(player, target, first);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.getArenaSetupManager().clearSessions(event.getPlayer().getUniqueId());
    }
}
