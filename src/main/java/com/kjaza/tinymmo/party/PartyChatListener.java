package com.kjaza.tinymmo.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PartyChatListener implements Listener {
    private final PartyManager pm;

    public PartyChatListener(PartyManager pm) {
        this.pm = pm;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        String msg = e.getMessage();
        if (!msg.startsWith("@")) {
            return;
        }
        Party party = pm.getPartyOf(e.getPlayer().getUniqueId());
        if (party == null) {
            return;
        }

        e.setCancelled(true);
        String clean = msg.substring(1).trim();
        if (clean.isEmpty()) {
            return;
        }
        String line = "§9[Party] §a" + e.getPlayer().getName() + "§7: §f" + clean;
        for (var id : party.members) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) {
                pl.sendMessage(line);
            }
        }
    }
}
