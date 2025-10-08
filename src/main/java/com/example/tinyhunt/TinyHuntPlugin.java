package com.example.tinyhunt;

import com.example.tinyhunt.command.TinyHuntCommand;
import com.example.tinyhunt.game.GameManager;
import com.example.tinyhunt.game.PlayerListener;
import com.kjaza.tinymmo.party.PartyChatCommand;
import com.kjaza.tinymmo.party.PartyChatListener;
import com.kjaza.tinymmo.party.PartyManager;
import com.kjaza.tinymmo.skill.CooldownManager;
import com.kjaza.tinymmo.skill.ResourceManager;
import com.kjaza.tinymmo.skill.SkillListener;
import com.kjaza.tinymmo.skill.SkillManager;
import com.kjaza.tinymmo.skill.VisualCooldowns;
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
    private PartyManager partyManager;
    private SkillManager skillManager;
    private CooldownManager cooldownManager;
    private ResourceManager resourceManager;
    private VisualCooldowns visualCooldowns;
    private int visualCooldownTaskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        gameManager = new GameManager(this);
        partyManager = new PartyManager();
        skillManager = new SkillManager(this);
        cooldownManager = new CooldownManager();
        resourceManager = new ResourceManager();
        visualCooldowns = new VisualCooldowns(this);
        TinyHuntCommand tinyHuntCommand = new TinyHuntCommand(this, gameManager);
        PluginCommand command = Objects.requireNonNull(getCommand("tinyhunt"),
                "tinyhunt command must be defined in plugin.yml");
        command.setExecutor(tinyHuntCommand);
        command.setTabCompleter(tinyHuntCommand);
        getServer().getPluginManager().registerEvents(new PlayerListener(gameManager), this);
        getServer().getPluginManager().registerEvents(
                new SkillListener(skillManager, cooldownManager, resourceManager, visualCooldowns, this),
                this);
        getServer().getPluginManager().registerEvents(new PartyChatListener(partyManager), this);
        PluginCommand partyChatCommand = Objects.requireNonNull(getCommand("p"),
                "p command must be defined in plugin.yml");
        partyChatCommand.setExecutor(new PartyChatCommand(partyManager));
        visualCooldownTaskId = getServer().getScheduler()
                .runTaskTimer(this, () -> visualCooldowns.tickActionbar(), 10L, 10L)
                .getTaskId();
        getLogger().info("TinyHunt plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (visualCooldownTaskId != -1) {
            getServer().getScheduler().cancelTask(visualCooldownTaskId);
            visualCooldownTaskId = -1;
        }
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

    public VisualCooldowns getVisualCooldowns() {
        return visualCooldowns;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }
}
