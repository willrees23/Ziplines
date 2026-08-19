package com.github.willrees23.listener;

import com.github.willrees23.zipline.ride.RideManager;
import com.github.willrees23.zipline.seat.SeatFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/**
 * Protects riders and their seats from the things that would otherwise interrupt a ride.
 */
public class ZiplineRideListener implements Listener {

    private final RideManager rides;
    private final SeatFactory seats;

    public ZiplineRideListener(RideManager rides, SeatFactory seats) {
        this.rides = rides;
        this.seats = seats;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (seats.isSeat(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (rides.isFallProtected(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Stops players from interacting with a seat, which would otherwise let them break the ride.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (seats.isSeat(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (rides.handleDismount(player, event.getDismounted())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            rides.handleSneak(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        rides.forget(event.getEntity());
    }
}
