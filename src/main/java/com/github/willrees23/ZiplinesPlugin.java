package com.github.willrees23;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class ZiplinesPlugin extends JavaPlugin {

    @Getter
    private static ZiplinesPlugin instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getLogger().info("ZiplinesPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("ZiplinesPlugin has been disabled.");
    }
}