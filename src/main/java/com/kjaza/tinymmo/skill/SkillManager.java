package com.kjaza.tinymmo.skill;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class SkillManager {
    private final Plugin plugin;
    private final Map<String, Skill> skills = new HashMap<>();
    private final NamespacedKey skillKey;
    private final NamespacedKey ownerKey;

    public SkillManager(Plugin plugin) {
        this.plugin = plugin;
        this.skillKey = new NamespacedKey(plugin, "skill_id");
        this.ownerKey = new NamespacedKey(plugin, "skill_owner");
    }

    public void register(Skill skill) {
        skills.put(skill.id(), skill);
    }

    public Optional<Skill> resolveSkill(ItemStack stack) {
        if (stack == null) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String id = container.get(skillKey, PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(skills.get(id));
    }

    public boolean isSkillItem(ItemStack stack) {
        return resolveSkill(stack).isPresent();
    }

    public boolean isOwner(Player player, ItemStack stack) {
        if (stack == null) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String owner = container.get(ownerKey, PersistentDataType.STRING);
        if (owner == null) {
            return true;
        }
        return owner.equalsIgnoreCase(player.getUniqueId().toString());
    }

    public NamespacedKey getSkillKey() {
        return skillKey;
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }
}
