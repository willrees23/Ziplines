package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.settings.RideDirection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;

import java.util.*;

/**
 * Keeps a block display parked at every end a zipline can be boarded from, so players can see where
 * to get on. A one way line therefore shows a seat at one end only.
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

        List<Location> locations = seatLocations(zipline);
        for (Location location : locations) {
            if (!isLoaded(location)) {
                return;
            }
        }

        List<BlockDisplay> spawned = new ArrayList<>();
        for (Location location : locations) {
            spawned.add(seats.spawnDisplay(location, zipline.getSettings()));
        }
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
     * Returns where this zipline wants its displays: one for each end a rider may board from.
     */
    private List<Location> seatLocations(Zipline zipline) {
        RideDirection direction = zipline.getSettings().getDirection();
        List<Location> locations = new ArrayList<>(2);
        if (direction.allowsStart()) {
            locations.add(seatLocation(zipline, zipline.getStart(), zipline.getEnd()));
        }
        if (direction.allowsEnd()) {
            locations.add(seatLocation(zipline, zipline.getEnd(), zipline.getStart()));
        }
        return locations;
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
