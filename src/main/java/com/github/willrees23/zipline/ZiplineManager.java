package com.github.willrees23.zipline;

import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.util.ChatUtil;
import com.github.willrees23.zipline.effect.PreviewTask;
import com.github.willrees23.zipline.path.PathClearance;
import com.github.willrees23.zipline.path.PathGeometry;
import com.github.willrees23.zipline.path.PathRenderer;
import com.github.willrees23.zipline.path.PathType;
import com.github.willrees23.zipline.ride.RideManager;
import com.github.willrees23.zipline.seat.SeatManager;
import com.github.willrees23.zipline.settings.ZiplineOption;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Owns the plugin's ziplines: creating them, editing them, deleting them and keeping the on-disk
 * copy in step.
 */
public class ZiplineManager {

    /**
     * The shortest line worth building; below this the two endpoints overlap in practice.
     */
    private static final double MIN_LENGTH = 2.0;

    /**
     * Ids are used as configuration keys and typed as command arguments, so they are held to
     * something that survives both: no dots to be read as a path, and no spaces to be split on.
     */
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    /**
     * Options that change how endpoint seats look or where they go, so the existing ones have to be
     * respawned.
     */
    private static final Set<ZiplineOption> SEAT_OPTIONS = Set.of(
            ZiplineOption.SEAT,
            ZiplineOption.DIRECTION,
            ZiplineOption.SEAT_MATERIAL,
            ZiplineOption.SEAT_SCALE,
            ZiplineOption.SEAT_OFFSET);

    private final Plugin plugin;
    private final ZiplineConfig config;
    private final ZiplineIndex index;
    private final ZiplineStorage storage;
    private final RideManager rides;
    private final SeatManager seats;

    /**
     * Finished ziplines, keyed by lower-cased id so that lookups are case insensitive.
     */
    private final Map<String, Zipline> ziplines = new LinkedHashMap<>();
    /**
     * Ziplines between {@code /zl start} and {@code /zl end}, keyed by the player building them.
     */
    private final Map<UUID, Zipline> pending = new LinkedHashMap<>();

    private final PreviewTask preview;

    public ZiplineManager(Plugin plugin,
                          ZiplineConfig config,
                          ZiplineIndex index,
                          ZiplineStorage storage,
                          RideManager rides,
                          SeatManager seats) {
        this.plugin = plugin;
        this.config = config;
        this.index = index;
        this.storage = storage;
        this.rides = rides;
        this.seats = seats;
        this.preview = new PreviewTask(plugin, pending);
    }

    public void load() {
        ziplines.clear();
        index.clear();
        for (Zipline zipline : storage.load(config.getDefaults())) {
            ziplines.put(key(zipline.getId()), zipline);
            index.add(zipline);
        }

        drawPaths();
        // Saved as soon as the paths are up, so that the snapshots just taken survive a crash. A
        // server that never reaches onDisable would otherwise leave blocks in the world with
        // nothing on disk to record what they replaced.
        save();

        plugin.getLogger().info("Loaded " + ziplines.size() + " ziplines.");
    }

    public void shutdown() {
        preview.stop();
        clearPaths();
        save();
    }

    public List<String> getIds() {
        return ziplines.values().stream().map(Zipline::getId).toList();
    }

    public Zipline getZipline(String id) {
        return ziplines.get(key(id));
    }

    public boolean isCreating(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    public void startCreation(Player player, String id, Double speed) {
        if (isCreating(player)) {
            ChatUtil.sendHighlighted(player,
                    "&7You are already creating a zipline. Use %s to finish or %s to cancel.", "/zl end", "/zl cancel");
            return;
        }
        if (!VALID_ID.matcher(id).matches()) {
            ChatUtil.sendHighlighted(player,
                    "&cThe name %s cannot be used. Names may use letters, digits, %s and %s.", id, "-", "_");
            return;
        }
        if (getZipline(id) != null) {
            ChatUtil.sendHighlighted(player, "&cA zipline named %s already exists.", id);
            return;
        }

        ZiplineSettings settings = config.getDefaults().copy();
        if (speed != null) {
            settings.setSpeed(speed);
        }

        pending.put(player.getUniqueId(), new Zipline(id, player.getEyeLocation(), settings));
        preview.start();
        ChatUtil.sendHighlighted(player, "&aStarted zipline %s with speed %s. Use %s to finish or %s to cancel.",
                id, settings.getSpeed(), "/zl end", "/zl cancel");
    }

    public void endCreation(Player player) {
        Zipline zipline = pending.get(player.getUniqueId());
        if (zipline == null) {
            ChatUtil.sendHighlighted(player, "&7You have not started creating a zipline. Use %s to begin.",
                    "/zl start <id>");
            return;
        }

        Location endLocation = player.getEyeLocation();
        if (!endLocation.getWorld().equals(zipline.getStart().getWorld())) {
            ChatUtil.sendColored(player, "&cA zipline must start and end in the same world.");
            return;
        }

        double length = endLocation.distance(zipline.getStart());
        if (length < MIN_LENGTH) {
            ChatUtil.sendHighlighted(player, "&cA zipline must be at least %s blocks long.", MIN_LENGTH);
            return;
        }
        if (length > config.getMaxLength()) {
            ChatUtil.sendHighlighted(player, "&cA zipline can be at most %s blocks long.", config.getMaxLength());
            return;
        }

        Location obstruction = PathClearance.firstObstruction(endLocation.getWorld(),
                PathGeometry.pathBlocks(zipline.getStart(), endLocation));
        if (obstruction != null) {
            ChatUtil.sendHighlighted(player,
                    "&cThe path is blocked at %s. A zipline needs %s blocks of clear space along its whole length.",
                    describe(obstruction), PathGeometry.CLEARANCE_DEPTH);
            return;
        }

        zipline.setEnd(endLocation);
        discardCreation(player);

        int replaced = zipline.getSettings().getPathType().getRenderer().apply(zipline);
        ziplines.put(key(zipline.getId()), zipline);
        index.add(zipline);
        save();

        ChatUtil.sendHighlighted(player, "&aCreated zipline %s with speed %s.",
                zipline.getId(), zipline.getSettings().getSpeed());
        if (replaced > 0) {
            ChatUtil.sendHighlighted(player, "&eThe path replaced %s existing blocks. Use %s to restore them.",
                    replaced, "/zl delete " + zipline.getId());
        }
    }

    public void cancelCreation(Player player) {
        if (!isCreating(player)) {
            ChatUtil.sendHighlighted(player, "&7You have not started creating a zipline. Use %s to begin.",
                    "/zl start <id>");
            return;
        }

        discardCreation(player);
        ChatUtil.sendColored(player, "&aZipline creation cancelled.");
    }

    /**
     * Drops any half-built zipline, and stops the preview once nobody is building one.
     */
    public void discardCreation(Player player) {
        pending.remove(player.getUniqueId());
        if (pending.isEmpty()) {
            preview.stop();
        }
    }

    public void delete(CommandSender sender, String id) {
        Zipline zipline = getZipline(id);
        if (zipline == null) {
            ChatUtil.sendHighlighted(sender, "&cNo zipline named %s exists.", id);
            return;
        }

        rides.stopRiders(zipline);
        seats.remove(zipline);
        zipline.getSettings().getPathType().getRenderer().remove(zipline);
        index.remove(zipline);
        ziplines.remove(key(id));
        save();

        ChatUtil.sendHighlighted(sender, "&aDeleted zipline %s and restored its path.", zipline.getId());
    }

    public void edit(CommandSender sender, String id, String key, String value) {
        Zipline zipline = getZipline(id);
        if (zipline == null) {
            ChatUtil.sendHighlighted(sender, "&cNo zipline named %s exists.", id);
            return;
        }

        ZiplineOption option = ZiplineOption.fromKey(key);
        if (option == null) {
            ChatUtil.sendHighlighted(sender, "&cUnknown option %s. Options: %s",
                    key, String.join(", ", ZiplineOption.keys()));
            return;
        }

        PathType previousType = zipline.getSettings().getPathType();
        if (!option.write(zipline.getSettings(), value)) {
            ChatUtil.sendHighlighted(sender, "&cInvalid value %s for option %s.", value, option.getKey());
            return;
        }

        if (SEAT_OPTIONS.contains(option)) {
            seats.remove(zipline);
        }

        if (option == ZiplineOption.PATH_TYPE || option == ZiplineOption.MATERIAL) {
            rides.stopRiders(zipline);
            previousType.getRenderer().remove(zipline);
            int replaced = zipline.getSettings().getPathType().getRenderer().apply(zipline);
            if (replaced > 0) {
                ChatUtil.sendHighlighted(sender, "&eThe path replaced %s existing blocks.", replaced);
            }
        }

        save();
        ChatUtil.sendHighlighted(sender, "&aSet %s to %s on zipline %s.",
                option.getKey(), option.read(zipline.getSettings()), zipline.getId());
    }

    public void list(CommandSender sender) {
        if (ziplines.isEmpty()) {
            ChatUtil.sendHighlighted(sender, "&7There are no ziplines yet. Use %s to create one.", "/zl start <id>");
            return;
        }

        ChatUtil.sendHighlighted(sender, "&7Ziplines (%s):", ziplines.size());
        for (Zipline zipline : ziplines.values()) {
            ChatUtil.sendHighlighted(sender, "&7- %s in %s, %s blocks, speed %s, %s path, %s trigger",
                    zipline.getId(),
                    zipline.getStart().getWorld().getName(),
                    Math.round(zipline.getLength()),
                    zipline.getSettings().getSpeed(),
                    zipline.getSettings().getPathType(),
                    zipline.getSettings().getTrigger());
        }
    }

    /**
     * Draws the path of every zipline that has one.
     *
     * <p>Paths go up on start-up and come down again on shutdown, so that the blocks only exist
     * while the plugin that knows about them is running. A zipline removed from {@code ziplines.yml}
     * while the server is down therefore leaves nothing behind in the world.
     *
     * <p>Each path is removed before it is drawn. After a clean shutdown that does nothing, since
     * the snapshot was emptied on the way out. After a crash it puts back whatever the leftover
     * blocks had replaced, so that the snapshot taken here still describes the untouched world
     * rather than the plugin's own leavings.
     */
    private void drawPaths() {
        for (Zipline zipline : ziplines.values()) {
            PathRenderer renderer = zipline.getSettings().getPathType().getRenderer();
            renderer.remove(zipline);
            renderer.apply(zipline);
        }
    }

    /**
     * Takes down every path, putting back whatever each one replaced.
     */
    private void clearPaths() {
        for (Zipline zipline : ziplines.values()) {
            zipline.getSettings().getPathType().getRenderer().remove(zipline);
        }
    }

    public void save() {
        storage.save(ziplines.values());
    }

    private String describe(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    private String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
