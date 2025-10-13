package com.example.tinyhunt.game;

import com.example.tinyhunt.TinyHuntPlugin;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Coordinates administrative setup flows such as arena creation and wand selections.
 */
public final class ArenaSetupManager {

    private static final String WAND_KEY = "arena-wand";

    private final TinyHuntPlugin plugin;
    private final GameManager gameManager;
    private final NamespacedKey wandIdentifier;
    private final Map<UUID, Boolean> pendingNamePrompts = new HashMap<>();
    private final Map<UUID, String> editingArenas = new HashMap<>();

    public ArenaSetupManager(TinyHuntPlugin plugin, GameManager gameManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
        this.wandIdentifier = new NamespacedKey(plugin, WAND_KEY);
    }

    public void requestArenaName(Player player) {
        pendingNamePrompts.put(player.getUniqueId(), Boolean.TRUE);
        player.sendMessage(plugin.getMessage("messages.arena-name-prompt"));
    }

    public boolean isAwaitingArenaName(UUID playerId) {
        return pendingNamePrompts.containsKey(playerId);
    }

    public void completeArenaName(Player player, String rawInput) {
        pendingNamePrompts.remove(player.getUniqueId());
        String sanitized = sanitizeName(rawInput);
        if (sanitized == null) {
            player.sendMessage(plugin.getMessage("messages.arena-name-invalid"));
            return;
        }
        if (gameManager.arenaExists(sanitized)) {
            player.sendMessage(plugin.getMessage("messages.arena-name-exists", Map.of("arena", sanitized)));
            return;
        }
        if (gameManager.createArena(sanitized)) {
            gameManager.setActiveArena(sanitized);
            player.sendMessage(plugin.getMessage("messages.arena-created", Map.of("arena", sanitized)));
        }
    }

    public void beginEditing(Player player, String arenaName) {
        editingArenas.put(player.getUniqueId(), arenaName);
        player.getInventory().addItem(createWand());
        player.sendMessage(plugin.getMessage("messages.arena-wand-received", Map.of("arena", arenaName)));
    }

    public Optional<String> getEditingArena(UUID playerId) {
        return Optional.ofNullable(editingArenas.get(playerId));
    }

    public void clearSessions(UUID playerId) {
        pendingNamePrompts.remove(playerId);
        editingArenas.remove(playerId);
    }

    public boolean handleSelection(Player player, Location location, boolean firstCorner) {
        String arenaName = editingArenas.get(player.getUniqueId());
        if (arenaName == null) {
            return false;
        }
        boolean saved = gameManager.saveArenaCorner(arenaName, location, firstCorner);
        if (!saved) {
            player.sendMessage(plugin.getMessage("messages.arena-not-found", Map.of("arena", arenaName)));
            return false;
        }
        player.sendMessage(plugin.getMessage("messages.arena-pos-set",
                Map.of("corner", firstCorner ? 1 : 2, "arena", arenaName)));
        return true;
    }

    public ItemStack createWand() {
        ItemStack itemStack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("TinyHunt Arena Wand", NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Sinistro: Pos1", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Destro: Pos2", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(wandIdentifier, PersistentDataType.BYTE, (byte) 1);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public boolean isSelectionWand(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.has(wandIdentifier, PersistentDataType.BYTE);
    }

    private String sanitizeName(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String sanitized = normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        sanitized = sanitized.replaceAll("-+", "-");
        sanitized = stripHyphen(sanitized);
        if (sanitized.isEmpty()) {
            return null;
        }
        return sanitized;
    }

    private String stripHyphen(String input) {
        int start = 0;
        int end = input.length();
        while (start < end && input.charAt(start) == '-') {
            start++;
        }
        while (end > start && input.charAt(end - 1) == '-') {
            end--;
        }
        return input.substring(start, end);
    }
}
