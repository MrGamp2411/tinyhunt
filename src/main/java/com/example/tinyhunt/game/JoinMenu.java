package com.example.tinyhunt.game;

import com.example.tinyhunt.TinyHuntPlugin;
import com.example.tinyhunt.model.ArenaDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Simple chest-based HUD used to visualise lobby status and allow players to join/leave.
 */
public final class JoinMenu {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int MENU_SIZE = 9;
    private static final int STATUS_SLOT = 3;
    private static final int ACTION_SLOT = 5;

    private final TinyHuntPlugin plugin;
    private final GameManager gameManager;

    public JoinMenu(TinyHuntPlugin plugin, GameManager gameManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, MENU_SIZE, menuTitle());
        inventory.setItem(STATUS_SLOT, buildStatusItem());
        inventory.setItem(ACTION_SLOT, buildActionItem(player));
        player.openInventory(inventory);
    }

    public boolean isMenu(InventoryView view) {
        return view != null && view.title().equals(menuTitle());
    }

    public boolean isActionSlot(int slot) {
        return slot == ACTION_SLOT;
    }

    public void handleAction(Player player) {
        if (gameManager.isInQueue(player)) {
            gameManager.leaveQueue(player);
            return;
        }
        gameManager.enqueue(player);
    }

    private Component menuTitle() {
        return LEGACY.deserialize(plugin.getMessage("messages.menu-title"));
    }

    private ItemStack buildStatusItem() {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(component("messages.menu-status-name"));
            List<Component> lore = new ArrayList<>();
            lore.add(component("messages.menu-status-state",
                    Map.of("state", plugin.getMessage("messages.state." + gameManager.getState().name().toLowerCase()))));
            lore.add(component("messages.menu-status-queue",
                    Map.of("queue", gameManager.getQueueSize(), "max", gameManager.getMaxPlayers())));
            lore.add(component("messages.menu-status-participants",
                    Map.of("participants", gameManager.getParticipantCount())));
            String arenaName = gameManager.getActiveArena().map(ArenaDefinition::getName)
                    .orElse(plugin.getMessage("messages.menu-no-arena"));
            lore.add(component("messages.menu-status-arena", Map.of("arena", arenaName)));
            meta.lore(lore);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack buildActionItem(Player player) {
        ItemStack itemStack;
        ItemMeta meta;
        if (!gameManager.getState().canJoin()) {
            itemStack = new ItemStack(Material.BARRIER);
            meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.displayName(component("messages.menu-join-locked"));
                meta.lore(componentList("messages.menu-join-locked-lore"));
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
        if (gameManager.isInQueue(player)) {
            itemStack = new ItemStack(Material.REDSTONE);
            meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.displayName(component("messages.menu-join-leave"));
                meta.lore(componentList("messages.menu-join-leave-lore"));
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
        if (gameManager.getQueueSize() >= gameManager.getMaxPlayers()) {
            itemStack = new ItemStack(Material.BARRIER);
            meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.displayName(component("messages.menu-join-full"));
                meta.lore(componentList("messages.menu-join-full-lore"));
                itemStack.setItemMeta(meta);
            }
            return itemStack;
        }
        itemStack = new ItemStack(Material.EMERALD);
        meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(component("messages.menu-join-available"));
            meta.lore(componentList("messages.menu-join-available-lore"));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private Component component(String path) {
        return component(path, Map.of());
    }

    private Component component(String path, Map<String, ?> placeholders) {
        return LEGACY.deserialize(plugin.getMessage(path, placeholders));
    }

    private List<Component> componentList(String path) {
        return componentList(path, Map.of());
    }

    private List<Component> componentList(String path, Map<String, ?> placeholders) {
        String raw = plugin.getMessage(path, placeholders);
        String[] split = raw.split("\\n");
        List<Component> components = new ArrayList<>();
        if (split.length == 0) {
            components.add(Component.empty());
            return components;
        }
        for (String line : split) {
            if (line.isEmpty()) {
                components.add(Component.empty());
            } else {
                components.add(LEGACY.deserialize(line));
            }
        }
        return components;
    }
}
