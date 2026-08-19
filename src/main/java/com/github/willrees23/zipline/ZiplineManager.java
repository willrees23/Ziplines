package com.github.willrees23.zipline;

import com.github.willrees23.ZiplinesPlugin;
import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.enums.PathType;
import com.github.willrees23.enums.ZiplineOption;
import com.github.willrees23.util.ChatUtil;
import com.github.willrees23.zipline.effect.ZiplineEffectTask;
import com.github.willrees23.zipline.path.PathClearance;
import com.github.willrees23.zipline.path.PathGeometry;
import com.github.willrees23.zipline.ride.RideManager;
import com.github.willrees23.zipline.seat.SeatManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ZiplineManager {

    private static final Particle PREVIEW_PARTICLE = Particle.CRIT;
    private static final double PREVIEW_STEP = 0.4;
    private static final long PREVIEW_INTERVAL = 1L;
    private static final int PREVIEW_MAX_POINTS = 200;
    private static final double MIN_LENGTH = 2.0;
    private static final Set<ZiplineOption> SEAT_OPTIONS = Set.of(
            ZiplineOption.SEAT,
            ZiplineOption.SEAT_MATERIAL,
            ZiplineOption.SEAT_SCALE,
            ZiplineOption.SEAT_OFFSET);

    private static ZiplineManager instance;

    private final Map<UUID, Zipline> ziplineCreationInProgress = new HashMap<>();
    private final List<Zipline> ziplines = new ArrayList<>();

    @Getter
    private final ZiplineConfig configuration = new ZiplineConfig();
    @Getter
    private final ZiplineIndex index = new ZiplineIndex();

    private final ZiplineEffectTask effectTask = new ZiplineEffectTask();

    private ZiplineStorage storage;
    private BukkitTask previewTask;

    public static ZiplineManager getInstance() {
        if (instance == null) {
            instance = new ZiplineManager();
        }
        return instance;
    }

    public void initialize(ZiplinesPlugin plugin) {
        configuration.load(plugin);
        storage = new ZiplineStorage(plugin);

        ziplines.clear();
        index.clear();
        for (Zipline zipline : storage.load(configuration.getDefaults())) {
            ziplines.add(zipline);
            index.add(zipline);
        }
        plugin.getLogger().info("Loaded " + ziplines.size() + " ziplines.");
        effectTask.start();
    }

    public void shutdown() {
        RideManager.getInstance().stopAll();
        SeatManager.getInstance().removeAll();
        effectTask.stop();
        if (previewTask != null) {
            previewTask.cancel();
            previewTask = null;
        }
        save();
    }

    public List<Zipline> getZiplines() {
        return List.copyOf(ziplines);
    }

    public Zipline getZipline(String id) {
        return ziplines.stream().filter(zipline -> zipline.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public boolean isCreatingZipline(Player player) {
        return ziplineCreationInProgress.containsKey(player.getUniqueId());
    }

    public boolean exists(String id) {
        return getZipline(id) != null;
    }

    public void startCreation(Player player, String id, Double speed) {
        if (isCreatingZipline(player)) {
            ChatUtil.sendHighlighted(player, "&7You are already creating a zipline. Use %s to finish or %s to cancel.", "/zl end", "/zl cancel");
            return;
        }
        if (exists(id)) {
            ChatUtil.sendHighlighted(player, "&cA zipline named %s already exists.", id);
            return;
        }

        ZiplineSettings settings = configuration.getDefaults().copy();
        if (speed != null) {
            settings.setSpeed(speed);
        }

        ziplineCreationInProgress.put(player.getUniqueId(), new Zipline(id, player.getEyeLocation(), settings));
        startPreviewTask();
        ChatUtil.sendHighlighted(player, "&aStarted zipline %s with speed %s. Use %s to finish or %s to cancel.", id, String.valueOf(settings.getSpeed()), "/zl end", "/zl cancel");
    }

    public void endCreation(Player player) {
        Zipline zipline = ziplineCreationInProgress.get(player.getUniqueId());
        if (zipline == null) {
            ChatUtil.sendHighlighted(player, "&7You haven't started creating a zipline. Use %s to begin.", "/zl start <id>");
            return;
        }

        Location endLocation = player.getEyeLocation();
        if (!endLocation.getWorld().equals(zipline.getStart().getWorld())) {
            ChatUtil.sendColored(player, "&cA zipline must start and end in the same world.");
            return;
        }

        double length = endLocation.distance(zipline.getStart());
        if (length < MIN_LENGTH) {
            ChatUtil.sendHighlighted(player, "&cA zipline must be at least %s blocks long.", String.valueOf(MIN_LENGTH));
            return;
        }
        if (length > configuration.getMaxLength()) {
            ChatUtil.sendHighlighted(player, "&cA zipline can be at most %s blocks long.", String.valueOf(configuration.getMaxLength()));
            return;
        }

        Location obstruction = PathClearance.firstObstruction(endLocation.getWorld(),
                PathGeometry.pathBlocks(zipline.getStart(), endLocation));
        if (obstruction != null) {
            ChatUtil.sendHighlighted(player, "&cThe path is blocked at %s. A zipline needs %s blocks of clear space along its whole length.",
                    describe(obstruction), String.valueOf(PathClearance.REQUIRED_HEIGHT));
            return;
        }

        zipline.setEnd(endLocation);
        discardCreation(player);

        int replaced = zipline.getSettings().getPathType().getRenderer().apply(zipline);
        ziplines.add(zipline);
        index.add(zipline);
        save();

        ChatUtil.sendHighlighted(player, "&aCreated zipline %s with speed %s.", zipline.getId(), String.valueOf(zipline.getSettings().getSpeed()));
        if (replaced > 0) {
            ChatUtil.sendHighlighted(player, "&eThe path replaced %s existing blocks. Use %s to restore them.", String.valueOf(replaced), "/zl delete " + zipline.getId());
        }
    }

    public void cancelCreation(Player player) {
        if (!isCreatingZipline(player)) {
            ChatUtil.sendHighlighted(player, "&7You haven't started creating a zipline. Use %s to begin.", "/zl start <id>");
            return;
        }

        discardCreation(player);
        ChatUtil.sendColored(player, "&aZipline creation cancelled.");
    }

    public void discardCreation(Player player) {
        ziplineCreationInProgress.remove(player.getUniqueId());
        stopPreviewTask();
    }

    public void delete(CommandSender sender, String id) {
        Zipline zipline = getZipline(id);
        if (zipline == null) {
            ChatUtil.sendHighlighted(sender, "&cNo zipline named %s exists.", id);
            return;
        }

        RideManager.getInstance().stopRiders(zipline);
        SeatManager.getInstance().remove(zipline);
        zipline.getSettings().getPathType().getRenderer().remove(zipline);
        index.remove(zipline);
        ziplines.remove(zipline);
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
            ChatUtil.sendHighlighted(sender, "&cUnknown option %s. Options: %s", key, String.join(", ", ZiplineOption.keys()));
            return;
        }

        PathType previousType = zipline.getSettings().getPathType();
        if (!zipline.getSettings().set(option, value)) {
            ChatUtil.sendHighlighted(sender, "&cInvalid value %s for option %s.", value, option.getKey());
            return;
        }

        if (SEAT_OPTIONS.contains(option)) {
            SeatManager.getInstance().remove(zipline);
        }

        if (option == ZiplineOption.PATH_TYPE || option == ZiplineOption.MATERIAL) {
            RideManager.getInstance().stopRiders(zipline);
            previousType.getRenderer().remove(zipline);
            int replaced = zipline.getSettings().getPathType().getRenderer().apply(zipline);
            if (replaced > 0) {
                ChatUtil.sendHighlighted(sender, "&eThe path replaced %s existing blocks.", String.valueOf(replaced));
            }
        }

        save();
        ChatUtil.sendHighlighted(sender, "&aSet %s to %s on zipline %s.", option.getKey(), zipline.getSettings().get(option), zipline.getId());
    }

    public void list(CommandSender sender) {
        if (ziplines.isEmpty()) {
            ChatUtil.sendHighlighted(sender, "&7There are no ziplines yet. Use %s to create one.", "/zl start <id>");
            return;
        }

        ChatUtil.sendHighlighted(sender, "&7Ziplines (%s):", String.valueOf(ziplines.size()));
        for (Zipline zipline : ziplines) {
            ChatUtil.sendHighlighted(sender, "&7- %s in %s, %s blocks, speed %s, %s path, %s trigger",
                    zipline.getId(),
                    zipline.getStart().getWorld().getName(),
                    String.valueOf(Math.round(zipline.getLength())),
                    String.valueOf(zipline.getSettings().getSpeed()),
                    zipline.getSettings().getPathType().name(),
                    zipline.getSettings().getTrigger().name());
        }
    }

    private String describe(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }

    public void save() {
        if (storage != null) {
            storage.save(ziplines);
        }
    }

    private void startPreviewTask() {
        if (previewTask != null) {
            return;
        }
        previewTask = Bukkit.getScheduler().runTaskTimer(ZiplinesPlugin.getInstance(), this::showPreviews, 0L, PREVIEW_INTERVAL);
    }

    private void stopPreviewTask() {
        if (previewTask == null || !ziplineCreationInProgress.isEmpty()) {
            return;
        }
        previewTask.cancel();
        previewTask = null;
    }

    private void showPreviews() {
        for (Map.Entry<UUID, Zipline> entry : ziplineCreationInProgress.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            showPreview(player, entry.getValue().getStart(), player.getEyeLocation());
        }
    }

    private void showPreview(Player player, Location start, Location end) {
        if (!start.getWorld().equals(end.getWorld())) {
            return;
        }

        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length < PREVIEW_STEP) {
            return;
        }

        int points = Math.min((int) (length / PREVIEW_STEP), PREVIEW_MAX_POINTS);
        Vector step = direction.normalize().multiply(length / points);
        Location point = start.clone();
        for (int i = 0; i < points; i++) {
            player.spawnParticle(PREVIEW_PARTICLE, point, 1, 0, 0, 0, 0);
            point.add(step);
        }
    }
}
