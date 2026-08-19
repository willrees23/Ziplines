package com.github.willrees23.zipline;

import com.github.willrees23.zipline.path.PlacedBlock;
import com.github.willrees23.zipline.settings.ZiplineOption;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reads and writes {@code ziplines.yml} in the plugin's data folder.
 *
 * <p>A zipline whose world is missing or whose stored data is unreadable is skipped with a warning
 * rather than aborting the load, so one bad entry cannot take the rest of them down with it.
 */
public class ZiplineStorage {

    private static final String FILE_NAME = "ziplines.yml";
    private static final String ROOT = "ziplines";
    private static final String SEPARATOR = ",";
    private static final int COORDINATES = 3;

    private final Plugin plugin;
    private final File file;

    public ZiplineStorage(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
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

            Location start = readLocation(world, section.getString("start"));
            Location end = readLocation(world, section.getString("end"));
            if (start == null || end == null) {
                plugin.getLogger().warning("Skipping zipline " + id + ": invalid location.");
                continue;
            }

            Zipline zipline = new Zipline(id, start, end, readSettings(section, defaults));
            zipline.setPlacedBlocks(readPlacedBlocks(section));
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

            ConfigurationSection section = config.createSection(ROOT + "." + zipline.getId());
            section.set("world", world.getName());
            section.set("start", writeLocation(zipline.getStart()));
            section.set("end", writeLocation(zipline.getEnd()));
            for (ZiplineOption option : ZiplineOption.values()) {
                section.set("settings." + option.getKey(), option.read(zipline.getSettings()));
            }
            section.set("blocks", zipline.getPlacedBlocks().stream().map(PlacedBlock::serialize).toList());
        }

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save ziplines: " + exception.getMessage());
        }
    }

    /** Reads the stored settings over a copy of the defaults, so new options pick up their default. */
    private ZiplineSettings readSettings(ConfigurationSection section, ZiplineSettings defaults) {
        ZiplineSettings settings = defaults.copy();
        ConfigurationSection stored = section.getConfigurationSection("settings");
        if (stored == null) {
            return settings;
        }

        for (ZiplineOption option : ZiplineOption.values()) {
            String value = stored.getString(option.getKey());
            if (value != null && !option.write(settings, value)) {
                plugin.getLogger().warning("Ignoring invalid " + option.getKey() + " on zipline "
                        + section.getName() + ": " + value);
            }
        }
        return settings;
    }

    private List<PlacedBlock> readPlacedBlocks(ConfigurationSection section) {
        List<PlacedBlock> blocks = new ArrayList<>();
        for (String stored : section.getStringList("blocks")) {
            PlacedBlock block = PlacedBlock.parse(stored);
            if (block != null) {
                blocks.add(block);
            }
        }
        return blocks;
    }

    private String writeLocation(Location location) {
        return location.getX() + SEPARATOR + location.getY() + SEPARATOR + location.getZ();
    }

    private Location readLocation(World world, String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.split(SEPARATOR);
        if (parts.length < COORDINATES) {
            return null;
        }
        try {
            return new Location(world, Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
