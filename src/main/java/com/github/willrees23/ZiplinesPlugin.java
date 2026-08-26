package com.github.willrees23;

import com.github.willrees23.command.ZiplineCancelCommand;
import com.github.willrees23.command.ZiplineDeleteCommand;
import com.github.willrees23.command.ZiplineEditCommand;
import com.github.willrees23.command.ZiplineEndCommand;
import com.github.willrees23.command.ZiplineExceptionHandler;
import com.github.willrees23.command.ZiplineId;
import com.github.willrees23.command.ZiplineListCommand;
import com.github.willrees23.command.ZiplineStartCommand;
import com.github.willrees23.command.ZiplinesCommand;
import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.listener.PlayerQuitListener;
import com.github.willrees23.listener.ZiplineProtectionListener;
import com.github.willrees23.listener.ZiplineRideListener;
import com.github.willrees23.listener.ZiplineTriggerListener;
import com.github.willrees23.zipline.ZiplineIndex;
import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.ZiplineProtection;
import com.github.willrees23.zipline.ZiplineStorage;
import com.github.willrees23.zipline.effect.ZiplineEffectTask;
import com.github.willrees23.zipline.ride.RideManager;
import com.github.willrees23.zipline.seat.SeatFactory;
import com.github.willrees23.zipline.seat.SeatManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

/**
 * Entry point for the plugin.
 *
 * <p>Everything is built here and passed to whatever needs it, rather than reached for through
 * static accessors, so that a reload starts from a clean set of objects and leaves nothing from the
 * previous run behind.
 */
public class ZiplinesPlugin extends JavaPlugin {

    private ZiplineManager ziplines;
    private RideManager rides;
    private SeatManager seats;
    private ZiplineEffectTask effects;

    @Override
    public void onEnable() {
        ZiplineConfig config = new ZiplineConfig(this);
        config.load();

        ZiplineIndex index = new ZiplineIndex();
        SeatFactory seatFactory = new SeatFactory(this);

        seats = new SeatManager(this, seatFactory);
        rides = new RideManager(this, config, index, seatFactory, seats);
        ziplines = new ZiplineManager(this, config, index, new ZiplineStorage(this), rides, seats);
        effects = new ZiplineEffectTask(this, index, seats);

        ziplines.load();
        effects.start();

        registerCommands();
        register(new PlayerQuitListener(ziplines, rides));
        register(new ZiplineTriggerListener(rides));
        register(new ZiplineRideListener(rides, seatFactory));
        register(new ZiplineProtectionListener(new ZiplineProtection(index), config));
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

    /**
     * Hands the command classes to Lamp, which builds {@code /ziplines} out of them and registers it
     * with the server. Nothing about the command appears in {@code plugin.yml}: adding a
     * sub-command means writing its class and naming it here.
     */
    private void registerCommands() {
        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
                // Fills the @Dependency field each command class declares.
                .dependency(ZiplineManager.class, ziplines)
                // Completions for @ZiplineId, which need the manager and so cannot be built reflectively.
                .suggestionProviders(providers -> providers.addProviderForAnnotation(
                        ZiplineId.class, annotation -> context -> ziplines.getIds()))
                .exceptionHandler(new ZiplineExceptionHandler())
                .build();

        lamp.register(
                new ZiplinesCommand(),
                new ZiplineStartCommand(),
                new ZiplineEndCommand(),
                new ZiplineCancelCommand(),
                new ZiplineDeleteCommand(),
                new ZiplineEditCommand(),
                new ZiplineListCommand()
        );
    }

    private void register(Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }
}
