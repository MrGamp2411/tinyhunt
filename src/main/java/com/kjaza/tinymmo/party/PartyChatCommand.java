package com.kjaza.tinymmo.party;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyChatCommand implements CommandExecutor {
    private final PartyManager pm;

    public PartyChatCommand(PartyManager pm) {
        this.pm = pm;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] args) {
        if (!(s instanceof Player p)) {
            s.sendMessage("Solo in-game.");
            return true;
        }
        Party party = pm.getPartyOf(p.getUniqueId());
        if (party == null) {
            p.sendMessage("§7Non sei in un party.");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage("§eUso: /p <messaggio>");
            return true;
        }

        String msg = String.join(" ", args);
        String line = "§9[Party] §a" + p.getName() + "§7: §f" + msg;
        for (var id : party.members) {
            Player pl = Bukkit.getPlayer(id);
            if (pl != null) {
                pl.sendMessage(line);
            }
        }
        return true;
    }
}
