package com.kjaza.tinymmo.skill;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class SkillListener implements Listener {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    private final SkillManager sm;
    private final CooldownManager cm;
    private final ResourceManager rm;
    private final VisualCooldowns vcd;
    private final Plugin plugin;

    public SkillListener(SkillManager sm, CooldownManager cm, ResourceManager rm,
            VisualCooldowns vcd, Plugin plugin) {
        this.sm = sm;
        this.cm = cm;
        this.rm = rm;
        this.vcd = vcd;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        ItemStack item = e.getItem();
        Optional<Skill> skillOpt = sm.resolveSkill(item);
        if (skillOpt.isEmpty()) {
            return;
        }
        Skill skill = skillOpt.get();

        if (!sm.isOwner(e.getPlayer(), item)) {
            e.setCancelled(true);
            return;
        }

        if (cm.isOnCooldown(e.getPlayer(), skill.id())) {
            int rem = cm.remaining(e.getPlayer(), skill.id());
            Component message = LEGACY_SERIALIZER.deserialize(ChatColor.RED + "Cooldown: " + rem + "s");
            e.getPlayer().sendActionBar(message);
            e.setCancelled(true);
            return;
        }

        String base = "skills.costs." + skill.id() + ".";
        ResourceType rt = ResourceType.from(
                plugin.getConfig().getString(base + "resource", skill.resource().key));
        int cost = plugin.getConfig().getInt(base + "amount", skill.cost());

        if (!rm.tryConsume(e.getPlayer(), rt, cost)) {
            Component message = LEGACY_SERIALIZER.deserialize(ChatColor.RED + "Non hai abbastanza "
                    + rt.key + " (" + cost + " richiesti).");
            e.getPlayer().sendActionBar(message);
            e.setCancelled(true);
            return;
        }

        skill.execute(e.getPlayer(), plugin);

        cm.start(e.getPlayer(), skill.id(), skill.cooldownSec());
        vcd.start(e.getPlayer(), skill, skill.cooldownSec());

        e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(),
                Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        e.setCancelled(true);
    }
}
