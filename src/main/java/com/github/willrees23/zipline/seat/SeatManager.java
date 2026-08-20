package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.settings.RideDirection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Keeps a block display parked at every end a zipline can be boarded from, so players can see where
 * to get on. A one way line therefore shows a seat at one end only.
 *
 * <p>A line that only one player may ride lends its display to that player's ride rather than
 * having a second seat spawned on top of it, so the seat they saw is the seat they leave on. It is
 * parked again by {@link #restore(Zipline)} once the ride is over, either straight away or, if the
 * line asks for it, by making its own way back down the line first.
 *
 * <p>These displays are not saved with the chunk, so they have to be respawned whenever the area is
 * reloaded. {@link #refresh(Zipline)} is called from the effect task for nearby ziplines and is
 * cheap when nothing needs doing.
 */
public class SeatManager {

    /**
     * How far a display may sit from where it parks and still count as parked, squared. Only a
     * display that has been carried off is worth sending back.
     */
    private static final double PARKED_TOLERANCE = 0.01;

    private final SeatFactory seats;
    private final SeatReturnTask returns;
    private final Map<String, List<BlockDisplay>> endpoints = new HashMap<>();

    public SeatManager(Plugin plugin, SeatFactory seats) {
        this.seats = seats;
        this.returns = new SeatReturnTask(plugin);
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
            returns.forget(zipline);
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

    /**
     * Lends out the display parked at {@code endpoint}, for a ride to carry along the line.
     *
     * @return the display to carry, or {@code null} if there is none parked there to lend
     */
    public BlockDisplay lend(Zipline zipline, Location endpoint) {
        List<BlockDisplay> displays = endpoints.get(zipline.getId());
        if (displays == null || returns.isReturning(zipline)) {
            return null;
        }

        List<Location> ends = boardableEnds(zipline);
        for (int i = 0; i < ends.size() && i < displays.size(); i++) {
            BlockDisplay display = displays.get(i);
            if (ends.get(i).equals(endpoint) && display.isValid()) {
                return display;
            }
        }
        return null;
    }

    /**
     * Parks every one of a zipline's displays where it belongs, which puts back the one a ride has
     * just carried off. A display that did not survive the ride is left to the next
     * {@link #refresh(Zipline)} to replace.
     *
     * <p>A line that asks its seats to make their own way back sets the carried one travelling
     * instead, and cannot be boarded again until it arrives.
     */
    public void restore(Zipline zipline) {
        List<BlockDisplay> displays = endpoints.get(zipline.getId());
        if (displays == null) {
            return;
        }

        boolean travels = zipline.getSettings().returnsEndpointSeat();
        List<Location> ends = boardableEnds(zipline);
        List<Location> locations = seatLocations(zipline);
        for (int i = 0; i < locations.size() && i < displays.size(); i++) {
            BlockDisplay display = displays.get(i);
            Location home = locations.get(i);
            if (!display.isValid()) {
                continue;
            }
            if (travels && hasLeft(display, home)) {
                returns.start(zipline, display, opposite(zipline, ends.get(i)), ends.get(i), home);
            } else {
                display.teleport(home);
            }
        }
    }

    /**
     * Whether a zipline is waiting on a seat to come back down the line, which is as good as it
     * being full: the one rider it takes cannot board until the seat is there to board.
     */
    public boolean isReturning(Zipline zipline) {
        return returns.isReturning(zipline);
    }

    public void remove(Zipline zipline) {
        returns.forget(zipline);
        List<BlockDisplay> displays = endpoints.remove(zipline.getId());
        if (displays != null) {
            despawn(displays);
        }
    }

    public void removeAll() {
        returns.forgetAll();
        for (String id : Set.copyOf(endpoints.keySet())) {
            despawn(endpoints.remove(id));
        }
    }

    /**
     * Returns the ends a rider may board this zipline from. Displays are held in this same order,
     * so an index into one list means the same end in the other.
     */
    private List<Location> boardableEnds(Zipline zipline) {
        RideDirection direction = zipline.getSettings().getDirection();
        List<Location> ends = new ArrayList<>(2);
        if (direction.allowsStart()) {
            ends.add(zipline.getStart());
        }
        if (direction.allowsEnd()) {
            ends.add(zipline.getEnd());
        }
        return ends;
    }

    /**
     * Returns where this zipline wants its displays: one for each end a rider may board from.
     */
    private List<Location> seatLocations(Zipline zipline) {
        List<Location> locations = new ArrayList<>(2);
        for (Location end : boardableEnds(zipline)) {
            Location facing = end == zipline.getStart() ? zipline.getEnd() : zipline.getStart();
            locations.add(seatLocation(zipline, end, facing));
        }
        return locations;
    }

    /**
     * The end of the line opposite the given one, which is where a seat lent out from it has to
     * make its way back from.
     */
    private Location opposite(Zipline zipline, Location end) {
        return end == zipline.getStart() ? zipline.getEnd() : zipline.getStart();
    }

    /**
     * Whether a display has been carried away from where it parks, rather than sitting there
     * untouched as the other end's display does throughout a ride.
     */
    private boolean hasLeft(BlockDisplay display, Location home) {
        Location location = display.getLocation();
        return location.getWorld() != null
                && location.getWorld().equals(home.getWorld())
                && location.distanceSquared(home) > PARKED_TOLERANCE;
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
