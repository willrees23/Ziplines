package com.github.willrees23.zipline.effect;

import com.github.willrees23.task.RepeatingTask;
import com.github.willrees23.zipline.Zipline;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;

/**
 * Shows each player building a zipline where the line would run, from its fixed start point to
 * wherever they are currently looking from.
 *
 * <p>Only runs while somebody is mid-build, and the preview is sent to that player alone.
 */
public final class PreviewTask {

    private static final Particle PARTICLE = Particle.CRIT;
    private static final long INTERVAL = 1L;
    private static final double STEP = 0.4;

    /**
     * Caps the work per tick on a long preview; the far end simply stops being drawn.
     */
    private static final int MAX_POINTS = 200;

    private final Map<UUID, Zipline> pending;
    private final RepeatingTask task;

    public PreviewTask(Plugin plugin, Map<UUID, Zipline> pending) {
        this.pending = pending;
        this.task = new RepeatingTask(plugin, INTERVAL, this::run);
    }

    public void start() {
        task.start();
    }

    public void stop() {
        task.stop();
    }

    private void run() {
        for (Map.Entry<UUID, Zipline> entry : pending.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                draw(player, entry.getValue().getStart(), player.getEyeLocation());
            }
        }
    }

    private void draw(Player player, Location start, Location end) {
        if (!start.getWorld().equals(end.getWorld())) {
            return;
        }

        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length < STEP) {
            return;
        }

        int points = Math.min((int) (length / STEP), MAX_POINTS);
        Vector step = direction.normalize().multiply(length / points);
        Location point = start.clone();
        for (int index = 0; index < points; index++) {
            player.spawnParticle(PARTICLE, point, 1, 0, 0, 0, 0);
            point.add(step);
        }
    }
}
