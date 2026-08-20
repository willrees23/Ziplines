package com.github.willrees23;

import com.github.willrees23.command.ZiplinesCommand;
import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.listener.PlayerQuitListener;
import com.github.willrees23.listener.ZiplineRideListener;
import com.github.willrees23.listener.ZiplineTriggerListener;
import com.github.willrees23.zipline.ZiplineIndex;
import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.ZiplineStorage;
import com.github.willrees23.zipline.effect.ZiplineEffectTask;
import com.github.willrees23.zipline.ride.RideManager;
import com.github.willrees23.zipline.seat.SeatFactory;
import com.github.willrees23.zipline.seat.SeatManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the plugin.
 *
 * <p>Everything is built here and passed to whatever needs it, rather than reached for through
 * static accessors, so that a reload starts from a clean set of objects and leaves nothing from the
 * previous run behind.
 */
public class ZiplinesPlugin extends JavaPlugin {

    private static final String COMMAND = "ziplines";

    private ZiplineManager ziplines;
    private RideManager rides;
    private SeatManager seats;
    private ZiplineEffectTask effects;

    @Override
    public void onEnable() {
        PluginCommand command = getCommand(COMMAND);
        if (command == null) {
            getLogger().severe("The " + COMMAND + " command is missing from plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ZiplineConfig config = new ZiplineConfig(this);
        config.load();

        ZiplineIndex index = new ZiplineIndex();
        SeatFactory seatFactory = new SeatFactory(this);

        seats = new SeatManager(seatFactory);
        rides = new RideManager(this, config, index, seatFactory, seats);
        ziplines = new ZiplineManager(this, config, index, new ZiplineStorage(this), rides, seats);
        effects = new ZiplineEffectTask(this, index, seats);

        ziplines.load();
        effects.start();

        command.setExecutor(new ZiplinesCommand(ziplines));
        register(new PlayerQuitListener(ziplines, rides));
        register(new ZiplineTriggerListener(rides));
        register(new ZiplineRideListener(rides, seatFactory));
    }

    @Override
    public void onDisable() {
        // Guarded because onDisable still runs if onEnable bailed out before wiring anything up.
        if (ziplines == null) {
            return;
        }

        rides.stopAll();
        seats.removeAll();
        effects.stop();
        ziplines.shutdown();
    }

    private void register(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
