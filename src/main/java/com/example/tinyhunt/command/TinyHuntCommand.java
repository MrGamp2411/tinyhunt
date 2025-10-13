package com.example.tinyhunt.command;

import com.example.tinyhunt.TinyHuntPlugin;
import com.example.tinyhunt.game.GameManager;
import com.example.tinyhunt.game.GameState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Handles the `/tinyhunt` root command and associated sub-commands.
 */
public final class TinyHuntCommand implements CommandExecutor, TabCompleter {

    private final TinyHuntPlugin plugin;
    private final GameManager gameManager;

    public TinyHuntCommand(TinyHuntPlugin plugin, GameManager gameManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("messages.command-help"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "join" -> handleJoin(sender);
            case "leave" -> handleLeave(sender);
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "reload" -> handleReload(sender);
            case "lobby" -> handleLobby(sender, args);
            case "arena" -> handleArena(sender, args);
            default -> sender.sendMessage(plugin.getMessage("messages.unknown-subcommand"));
        }
        return true;
    }

    private void handleJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return;
        }
        if (!sender.hasPermission("tinyhunt.play")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        gameManager.getJoinMenu().open(player);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return;
        }
        if (!sender.hasPermission("tinyhunt.play")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        gameManager.leaveQueue(player);
    }

    private void handleStart(CommandSender sender) {
        if (!sender.hasPermission("tinyhunt.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        if (gameManager.getState() == GameState.RUNNING) {
            sender.sendMessage(plugin.getMessage("messages.already-running"));
            return;
        }
        if (gameManager.forceStart()) {
            sender.sendMessage(plugin.getMessage("messages.start-requested"));
        } else {
            sender.sendMessage(plugin.getMessage("messages.not-enough-players"));
        }
    }

    private void handleStop(CommandSender sender) {
        if (!sender.hasPermission("tinyhunt.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        if (gameManager.stopManually()) {
            sender.sendMessage(plugin.getMessage("messages.stop-requested"));
        } else {
            sender.sendMessage(plugin.getMessage("messages.no-active-game"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("tinyhunt.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        gameManager.reloadSettings();
        sender.sendMessage(plugin.getMessage("messages.reloaded"));
    }

    private void handleLobby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return;
        }
        if (!sender.hasPermission("tinyhunt.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("messages.usage-lobby"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "setpos1" -> {
                gameManager.saveLobbyCorner(player.getLocation(), true);
                sender.sendMessage(plugin.getMessage("messages.lobby-pos-set", Map.of("corner", 1)));
            }
            case "setpos2" -> {
                gameManager.saveLobbyCorner(player.getLocation(), false);
                sender.sendMessage(plugin.getMessage("messages.lobby-pos-set", Map.of("corner", 2)));
            }
            default -> sender.sendMessage(plugin.getMessage("messages.usage-lobby"));
        }
    }

    private void handleArena(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessage("messages.player-only"));
            return;
        }
        if (!sender.hasPermission("tinyhunt.admin")) {
            sender.sendMessage(plugin.getMessage("messages.no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessage("messages.usage-arena"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> handleArenaCreate(player);
            case "set" -> handleArenaSet(player, args);
            case "spawn" -> handleArenaSpawn(player, args);
            default -> sender.sendMessage(plugin.getMessage("messages.usage-arena"));
        }
    }

    private void handleArenaCreate(Player player) {
        gameManager.getArenaSetupManager().requestArenaName(player);
    }

    private void handleArenaSet(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("messages.usage-arena"));
            return;
        }
        String arenaName = args[2].toLowerCase(Locale.ROOT);
        if (!gameManager.arenaExists(arenaName)) {
            player.sendMessage(plugin.getMessage("messages.arena-not-found", Map.of("arena", arenaName)));
            return;
        }
        gameManager.setActiveArena(arenaName);
        gameManager.getArenaSetupManager().beginEditing(player, arenaName);
    }

    private void handleArenaSpawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("messages.usage-arena"));
            return;
        }
        String arenaName = args[2].toLowerCase(Locale.ROOT);
        if (!gameManager.addArenaSpawn(arenaName, player.getLocation())) {
            player.sendMessage(plugin.getMessage("messages.arena-not-found", Map.of("arena", arenaName)));
            return;
        }
        player.sendMessage(plugin.getMessage("messages.arena-spawn-added", Map.of("arena", arenaName)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("tinyhunt.play")) {
                options.add("join");
                options.add("leave");
            }
            if (sender.hasPermission("tinyhunt.admin")) {
                options.addAll(Arrays.asList("start", "stop", "reload", "lobby", "arena"));
            }
            return partialMatches(args[0], options);
        }
        if (args.length == 2) {
            if ("lobby".equalsIgnoreCase(args[0]) && sender.hasPermission("tinyhunt.admin")) {
                return partialMatches(args[1], Arrays.asList("setpos1", "setpos2"));
            }
            if ("arena".equalsIgnoreCase(args[0]) && sender.hasPermission("tinyhunt.admin")) {
                return partialMatches(args[1], Arrays.asList("create", "set", "spawn"));
            }
        }
        if (args.length == 3) {
            if ("arena".equalsIgnoreCase(args[0]) && sender.hasPermission("tinyhunt.admin")) {
                if ("set".equalsIgnoreCase(args[1]) || "spawn".equalsIgnoreCase(args[1])) {
                    return partialMatches(args[2], gameManager.getArenaNames());
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> partialMatches(String token, List<String> options) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
