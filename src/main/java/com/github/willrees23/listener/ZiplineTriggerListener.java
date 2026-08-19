package com.github.willrees23.listener;

import com.github.willrees23.zipline.ride.RideManager;
import com.github.willrees23.zipline.settings.TriggerMode;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Watches for the two ways a player can board a zipline. */
public class ZiplineTriggerListener implements Listener {

    private final RideManager rides;

    public ZiplineTriggerListener(RideManager rides) {
        this.rides = rides;
    }

    /**
     * Move events fire several times per tick for a player who is merely looking around, so the
     * check is skipped unless they have actually crossed into a different block.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        rides.tryStart(event.getPlayer(), TriggerMode.WALK);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Right clicks fire once per hand; the off hand is ignored so a ride only starts once.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        rides.tryStart(event.getPlayer(), TriggerMode.RIGHT_CLICK);
    }
}
