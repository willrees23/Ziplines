package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * The pair of entities carrying one rider along a line.
 */
public class RideSeat {

    /**
     * Ignore measured mount offsets beyond this, which is further than any vanilla mount sits and so
     * means the player has been moved by something other than the ride.
     */
    private static final double MAX_MOUNT_OFFSET = 2.0;

    private final ArmorStand vehicle;
    private final BlockDisplay display;
    private final Entity seat;

    private double mountOffset;

    public RideSeat(SeatFactory seats, Player player, ZiplineSettings settings, Location start) {
        this.vehicle = seats.spawnVehicle(start);
        this.display = settings.isSeat() ? seats.spawnDisplay(start, settings) : null;

        if (display == null) {
            this.seat = vehicle;
        } else {
            vehicle.addPassenger(display);
            this.seat = display;
        }
        this.seat.addPassenger(player);
    }

    public boolean isMounted(Player player) {
        return vehicle.isValid() && seat.isValid() && seat.getPassengers().contains(player);
    }

    public boolean isVehicle(Entity entity) {
        return seat.equals(entity) || vehicle.equals(entity);
    }

    /**
     * Drives the vehicle towards {@code target}.
     *
     * <p>A passenger rides some distance above whatever carries it, and that distance depends on the
     * seat's block and scale. Rather than predict it, the offset is measured from where the player
     * actually ended up and subtracted from the target, so the player rather than the vehicle lands
     * on the line.
     */
    public void move(Player player, Location target) {
        if (!vehicle.isValid()) {
            return;
        }

        double measured = player.getLocation().getY() - vehicle.getLocation().getY();
        if (measured >= 0 && measured <= MAX_MOUNT_OFFSET) {
            mountOffset = measured;
        }

        Location vehicleTarget = target.clone().subtract(0, mountOffset, 0);
        vehicle.setVelocity(vehicleTarget.toVector().subtract(vehicle.getLocation().toVector()));
    }

    public void release(Player player) {
        seat.removePassenger(player);
        if (display != null) {
            display.remove();
        }
        vehicle.remove();
    }
}
