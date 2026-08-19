package com.github.willrees23.zipline.effect;

import com.github.willrees23.task.RepeatingTask;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineIndex;
import com.github.willrees23.zipline.seat.SeatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * Keeps nearby ziplines looking alive: respawns their endpoint seats, lets particle paths draw
 * themselves, and spins a ring of particles around each endpoint.
 *
 * <p>Work is driven from the players outwards rather than over every zipline, so a server with many
 * ziplines only pays for the ones somebody can actually see.
 */
public final class ZiplineEffectTask {

    private static final long INTERVAL = 4L;
    private static final double RADIUS = 32.0;
    private static final double ENDPOINT_RADIUS = 0.6;
    private static final int ENDPOINT_POINTS = 4;
    private static final double ROTATION_STEP = Math.PI / 8;

    private final ZiplineIndex index;
    private final SeatManager seats;
    private final RepeatingTask task;

    private int ticks;

    public ZiplineEffectTask(Plugin plugin, ZiplineIndex index, SeatManager seats) {
        this.index = index;
        this.seats = seats;
        this.task = new RepeatingTask(plugin, INTERVAL, this::run);
    }

    public void start() {
        task.start();
    }

    public void stop() {
        task.stop();
    }

    private void run() {
        ticks++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<Zipline> nearby = index.nearby(player.getLocation(), RADIUS);
            for (Zipline zipline : nearby) {
                seats.refresh(zipline);
                zipline.getSettings().getPathType().getRenderer().tick(zipline, player);
                drawEndpoint(player, zipline, zipline.getStart());
                drawEndpoint(player, zipline, zipline.getEnd());
            }
        }
    }

    /**
     * Draws a slowly rotating ring around an endpoint. The index only narrows candidates down to a
     * chunk, so the distance is checked again here.
     */
    private void drawEndpoint(Player player, Zipline zipline, Location endpoint) {
        if (endpoint.distanceSquared(player.getLocation()) > RADIUS * RADIUS) {
            return;
        }

        Particle particle = zipline.getSettings().getEndpointParticle();
        double offset = ticks * ROTATION_STEP;
        Location point = endpoint.clone();
        for (int corner = 0; corner < ENDPOINT_POINTS; corner++) {
            double angle = offset + corner * (2 * Math.PI / ENDPOINT_POINTS);
            point.setX(endpoint.getX() + Math.cos(angle) * ENDPOINT_RADIUS);
            point.setZ(endpoint.getZ() + Math.sin(angle) * ENDPOINT_RADIUS);
            point.setY(endpoint.getY() + Math.sin(offset) * ENDPOINT_RADIUS);
            player.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
}
