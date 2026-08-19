package com.github.willrees23.zipline;

import com.github.willrees23.enums.ZiplineOption;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ZiplineStorage {

    private static final String ROOT = "ziplines";

    private final JavaPlugin plugin;
    private final File file;

    public ZiplineStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ziplines.yml");
    }

    public List<Zipline> load(ZiplineSettings defaults) {
        List<Zipline> loaded = new ArrayList<>();
        if (!file.exists()) {
            return loaded;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection(ROOT);
        if (root == null) {
            return loaded;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            World world = Bukkit.getWorld(section.getString("world", ""));
            if (world == null) {
                plugin.getLogger().warning("Skipping zipline " + id + ": unknown world.");
                continue;
            }

            Location start = deserialize(world, section.getString("start"));
            Location end = deserialize(world, section.getString("end"));
            if (start == null || end == null) {
                plugin.getLogger().warning("Skipping zipline " + id + ": invalid location.");
                continue;
            }

            ZiplineSettings settings = defaults.copy();
            ConfigurationSection settingsSection = section.getConfigurationSection("settings");
            if (settingsSection != null) {
                for (ZiplineOption option : ZiplineOption.values()) {
                    String value = settingsSection.getString(option.getKey());
                    if (value != null) {
                        settings.set(option, value);
                    }
                }
            }

            Zipline zipline = new Zipline(id, start, end, settings);
            zipline.setPlacedBlocks(new ArrayList<>(section.getStringList("blocks")));
            loaded.add(zipline);
        }
        return loaded;
    }

    public void save(Collection<Zipline> ziplines) {
        YamlConfiguration config = new YamlConfiguration();
        for (Zipline zipline : ziplines) {
            World world = zipline.getStart().getWorld();
            if (world == null) {
                continue;
            }

            String base = ROOT + "." + zipline.getId();
            config.set(base + ".world", world.getName());
            config.set(base + ".start", serialize(zipline.getStart()));
            config.set(base + ".end", serialize(zipline.getEnd()));
            for (ZiplineOption option : ZiplineOption.values()) {
                config.set(base + ".settings." + option.getKey(), zipline.getSettings().get(option));
            }
            config.set(base + ".blocks", zipline.getPlacedBlocks());
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save ziplines: " + exception.getMessage());
        }
    }

    private String serialize(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ();
    }

    private Location deserialize(World world, String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(",");
        if (parts.length < 3) {
            return null;
        }
        try {
            return new Location(world, Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
