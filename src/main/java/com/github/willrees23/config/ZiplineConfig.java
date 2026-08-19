package com.github.willrees23.config;

import com.github.willrees23.zipline.settings.ZiplineOption;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The contents of {@code config.yml}: the server-wide defaults for new ziplines.
 */
public class ZiplineConfig {

    private static final double DEFAULT_TRIGGER_RADIUS = 1.5;
    private static final double MIN_TRIGGER_RADIUS = 0.5;
    private static final int DEFAULT_MAX_LENGTH = 256;
    private static final int MIN_MAX_LENGTH = 2;

    private final JavaPlugin plugin;

    /**
     * Settings a newly created zipline starts with.
     */
    @Getter
    private ZiplineSettings defaults = new ZiplineSettings();

    /**
     * How close to an endpoint a player has to be for the trigger to fire.
     */
    @Getter
    private double triggerRadius = DEFAULT_TRIGGER_RADIUS;

    /**
     * Longest zipline that may be built, which also bounds the work each path operation does.
     */
    @Getter
    private int maxLength = DEFAULT_MAX_LENGTH;

    public ZiplineConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        defaults = new ZiplineSettings();
        ConfigurationSection section = config.getConfigurationSection("defaults");
        if (section != null) {
            for (ZiplineOption option : ZiplineOption.values()) {
                String value = section.getString(option.getKey());
                if (value != null && !option.write(defaults, value)) {
                    plugin.getLogger().warning("Invalid default for " + option.getKey() + ": " + value);
                }
            }
        }

        triggerRadius = Math.max(MIN_TRIGGER_RADIUS, config.getDouble("trigger-radius", DEFAULT_TRIGGER_RADIUS));
        maxLength = Math.max(MIN_MAX_LENGTH, config.getInt("max-length", DEFAULT_MAX_LENGTH));
    }
}
