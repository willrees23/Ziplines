package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.Zipline;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;

import java.util.*;

/**
 * Keeps a block display parked at each end of every zipline, so players can see where a line can be
 * boarded.
 *
 * <p>These displays are not saved with the chunk, so they have to be respawned whenever the area is
 * reloaded. {@link #refresh(Zipline)} is called from the effect task for nearby ziplines and is
 * cheap when nothing needs doing.
 */
public class SeatManager {

    private final SeatFactory seats;
    private final Map<String, List<BlockDisplay>> endpoints = new HashMap<>();

    public SeatManager(SeatFactory seats) {
        this.seats = seats;
    }

    public void refresh(Zipline zipline) {
        if (!zipline.getSettings().isSeat()) {
            remove(zipline);
            return;
        }

        List<BlockDisplay> existing = endpoints.get(zipline.getId());
        if (existing != null) {
            if (isIntact(existing)) {
                return;
            }
            despawn(existing);
            endpoints.remove(zipline.getId());
        }

        Location start = seatLocation(zipline, zipline.getStart(), zipline.getEnd());
        Location end = seatLocation(zipline, zipline.getEnd(), zipline.getStart());
        if (!isLoaded(start) || !isLoaded(end)) {
            return;
        }

        List<BlockDisplay> spawned = new ArrayList<>();
        spawned.add(seats.spawnDisplay(start, zipline.getSettings()));
        spawned.add(seats.spawnDisplay(end, zipline.getSettings()));
        endpoints.put(zipline.getId(), spawned);
    }

    public void remove(Zipline zipline) {
        List<BlockDisplay> displays = endpoints.remove(zipline.getId());
        if (displays != null) {
            despawn(displays);
        }
    }

    public void removeAll() {
        for (String id : Set.copyOf(endpoints.keySet())) {
            despawn(endpoints.remove(id));
        }
    }

    /**
     * Places the display at ride height, turned to face along the line.
     */
    private Location seatLocation(Zipline zipline, Location endpoint, Location facing) {
        Location location = endpoint.clone();
        location.setY(zipline.getProfile().rideHeight(endpoint.getX(), endpoint.getZ(), endpoint.getY()));
        location.setYaw(yaw(endpoint, facing));
        location.setPitch(0);
        return location;
    }

    private float yaw(Location from, Location to) {
        double x = to.getX() - from.getX();
        double z = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    private boolean isLoaded(Location location) {
        World world = location.getWorld();
        return world != null && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private boolean isIntact(List<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            if (!display.isValid()) {
                return false;
            }
        }
        return true;
    }

    private void despawn(List<BlockDisplay> displays) {
        for (BlockDisplay display : displays) {
            display.remove();
        }
    }
}
