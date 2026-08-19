package com.github.willrees23.config;

import com.github.willrees23.enums.ZiplineOption;
import com.github.willrees23.zipline.ZiplineSettings;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ZiplineConfig {

    private ZiplineSettings defaults = new ZiplineSettings();
    private double triggerRadius = 1.5;
    private int maxLength = 256;

    public void load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        defaults = new ZiplineSettings();
        ConfigurationSection section = config.getConfigurationSection("defaults");
        if (section != null) {
            for (ZiplineOption option : ZiplineOption.values()) {
                String value = section.getString(option.getKey());
                if (value != null && !defaults.set(option, value)) {
                    plugin.getLogger().warning("Invalid default for " + option.getKey() + ": " + value);
                }
            }
        }

        triggerRadius = Math.max(0.5, config.getDouble("trigger-radius", triggerRadius));
        maxLength = Math.max(2, config.getInt("max-length", maxLength));
    }
}
