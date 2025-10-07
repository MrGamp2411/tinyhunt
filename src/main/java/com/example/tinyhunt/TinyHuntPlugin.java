package com.example.tinyhunt;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the TinyHunt Paper plugin.
 */
public final class TinyHuntPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("TinyHunt plugin enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TinyHunt plugin disabled.");
    }
}
