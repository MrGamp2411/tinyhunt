package com.example.tinyhunt;

import com.example.tinyhunt.command.TinyHuntCommand;
import com.example.tinyhunt.game.GameManager;
import com.example.tinyhunt.game.PlayerListener;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the TinyHunt Paper plugin.
 */
public final class TinyHuntPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        gameManager = new GameManager(this);
        TinyHuntCommand tinyHuntCommand = new TinyHuntCommand(this, gameManager);
        PluginCommand command = Objects.requireNonNull(getCommand("tinyhunt"),
                "tinyhunt command must be defined in plugin.yml");
        command.setExecutor(tinyHuntCommand);
        command.setTabCompleter(tinyHuntCommand);
        getServer().getPluginManager().registerEvents(new PlayerListener(gameManager), this);
        getLogger().info("TinyHunt plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cancelAllTasks();
        }
        getLogger().info("TinyHunt plugin disabled.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public String getMessage(String path) {
        String raw = getConfig().getString(path, path);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getMessage(String path, Map<String, ?> placeholders) {
        String message = getMessage(path);
        if (placeholders == null || placeholders.isEmpty()) {
            return message;
        }
        for (Entry<String, ?> entry : placeholders.entrySet()) {
            String token = "%" + entry.getKey() + "%";
            message = message.replace(token, String.valueOf(entry.getValue()));
        }
        return message;
    }
}
