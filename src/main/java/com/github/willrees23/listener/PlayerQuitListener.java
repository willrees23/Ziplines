package com.github.willrees23.listener;

import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.ride.RideManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clears out anything a leaving player was in the middle of, so nothing is left holding their id.
 */
public class PlayerQuitListener implements Listener {

    private final ZiplineManager ziplines;
    private final RideManager rides;

    public PlayerQuitListener(ZiplineManager ziplines, RideManager rides) {
        this.ziplines = ziplines;
        this.rides = rides;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ziplines.discardCreation(event.getPlayer());
        rides.forget(event.getPlayer());
    }
}
