package com.github.willrees23.zipline.effect;

import com.github.willrees23.ZiplinesPlugin;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.seat.SeatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

public class ZiplineEffectTask {

    private static final long EFFECT_INTERVAL = 4L;
    private static final double EFFECT_RADIUS = 32.0;
    private static final double ENDPOINT_RADIUS = 0.6;
    private static final int ENDPOINT_POINTS = 4;
    private static final double ROTATION_STEP = Math.PI / 8;

    private BukkitTask task;
    private int ticks;

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(ZiplinesPlugin.getInstance(), this::run, 0L, EFFECT_INTERVAL);
    }

    public void stop() {
        if (task == null) {
            return;
        }
        task.cancel();
        task = null;
    }

    private void run() {
        ticks++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<Zipline> nearby = ZiplineManager.getInstance().getIndex().nearby(player.getLocation(), EFFECT_RADIUS);
            for (Zipline zipline : nearby) {
                SeatManager.getInstance().refresh(zipline);
                zipline.getSettings().getPathType().getRenderer().tick(zipline, player);
                drawEndpoint(player, zipline, zipline.getStart());
                drawEndpoint(player, zipline, zipline.getEnd());
            }
        }
    }

    private void drawEndpoint(Player player, Zipline zipline, Location endpoint) {
        if (endpoint.distanceSquared(player.getLocation()) > EFFECT_RADIUS * EFFECT_RADIUS) {
            return;
        }

        Particle particle = zipline.getSettings().getEndpointParticle();
        double offset = ticks * ROTATION_STEP;
        Location point = endpoint.clone();
        for (int index = 0; index < ENDPOINT_POINTS; index++) {
            double angle = offset + index * (2 * Math.PI / ENDPOINT_POINTS);
            point.setX(endpoint.getX() + Math.cos(angle) * ENDPOINT_RADIUS);
            point.setZ(endpoint.getZ() + Math.sin(angle) * ENDPOINT_RADIUS);
            point.setY(endpoint.getY() + Math.sin(offset) * ENDPOINT_RADIUS);
            player.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
}
