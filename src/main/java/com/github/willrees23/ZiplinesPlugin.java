package com.github.willrees23;

import com.github.willrees23.command.ZiplinesCommand;
import com.github.willrees23.listener.PlayerQuitListener;
import com.github.willrees23.listener.ZiplineRideListener;
import com.github.willrees23.listener.ZiplineTriggerListener;
import com.github.willrees23.zipline.ZiplineManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class ZiplinesPlugin extends JavaPlugin {

    @Getter
    private static ZiplinesPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("ZiplinesPlugin has been enabled!");

        ZiplineManager.getInstance().initialize(this);

        getCommand("ziplines").setExecutor(new ZiplinesCommand());
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(), this);
        getServer().getPluginManager().registerEvents(new ZiplineTriggerListener(), this);
        getServer().getPluginManager().registerEvents(new ZiplineRideListener(), this);
    }

    @Override
    public void onDisable() {
        ZiplineManager.getInstance().shutdown();
        getLogger().info("ZiplinesPlugin has been disabled.");
    }
}
