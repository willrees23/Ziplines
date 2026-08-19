package com.github.willrees23.listener;

import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.ride.RideManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ZiplineManager.getInstance().discardCreation(event.getPlayer());
        RideManager.getInstance().forget(event.getPlayer());
    }
}
