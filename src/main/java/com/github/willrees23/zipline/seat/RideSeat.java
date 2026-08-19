package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.ZiplineSettings;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class RideSeat {

    private static final double MAX_MOUNT_OFFSET = 2.0;

    private final ArmorStand vehicle;
    private final BlockDisplay display;
    private final Entity seat;

    private double mountOffset;

    public RideSeat(Player player, ZiplineSettings settings, Location start) {
        this.vehicle = SeatEntities.spawnVehicle(start);
        this.display = settings.isSeat() ? SeatEntities.spawnDisplay(start, settings) : null;

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
