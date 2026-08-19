package com.github.willrees23.zipline.ride;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.task.RepeatingTask;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineIndex;
import com.github.willrees23.zipline.seat.SeatFactory;
import com.github.willrees23.zipline.settings.ExitMode;
import com.github.willrees23.zipline.settings.TriggerMode;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Tracks who is currently riding, starts new rides, and cleans up when one ends.
 */
public final class RideManager {

    private static final long RIDE_INTERVAL = 1L;

    /**
     * Keeps a player who has just been dropped at an endpoint from immediately boarding again.
     */
    private static final long TRIGGER_COOLDOWN_MILLIS = 1000L;

    /**
     * How long after a ride a player stays safe from fall damage caused by the drop.
     */
    private static final long FALL_GRACE_MILLIS = 10000L;

    /**
     * Fraction of ride speed carried into the drop, so riders do not stop dead at the end.
     */
    private static final double DROP_CARRY = 0.5;

    /**
     * Minimum upward share of a launch, so a launch off a level line still throws the rider up.
     */
    private static final double LAUNCH_LIFT = 0.35;

    private final ZiplineConfig config;
    private final ZiplineIndex index;
    private final SeatFactory seats;

    private final Map<UUID, ZiplineRide> rides = new HashMap<>();
    private final Map<UUID, Long> triggerCooldowns = new HashMap<>();
    private final Map<UUID, Long> fallGrace = new HashMap<>();

    private final RepeatingTask task;

    public RideManager(Plugin plugin, ZiplineConfig config, ZiplineIndex index, SeatFactory seats) {
        this.config = config;
        this.index = index;
        this.seats = seats;
        this.task = new RepeatingTask(plugin, RIDE_INTERVAL, this::tickRides);
    }

    public boolean isRiding(Player player) {
        return rides.containsKey(player.getUniqueId());
    }

    /**
     * Starts a ride if the player is stood at the end of a zipline that uses the given trigger.
     *
     * <p>Both ends of every candidate are considered, and the nearest wins, so a line can be ridden
     * in either direction.
     */
    public void tryStart(Player player, TriggerMode mode) {
        if (isRiding(player) || isOnCooldown(player)
                || !player.hasPermission(ZiplinePermission.ZIPLINE_USE.getNode())) {
            return;
        }

        double radius = config.getTriggerRadius();
        Location eyes = player.getEyeLocation();

        Zipline closest = null;
        Location from = null;
        Location to = null;
        double closestDistance = radius * radius;
        for (Zipline zipline : index.nearby(eyes, radius)) {
            if (zipline.getSettings().getTrigger() != mode) {
                continue;
            }
            double toStart = zipline.getStart().distanceSquared(eyes);
            if (toStart < closestDistance) {
                closest = zipline;
                from = zipline.getStart();
                to = zipline.getEnd();
                closestDistance = toStart;
            }
            double toEnd = zipline.getEnd().distanceSquared(eyes);
            if (toEnd < closestDistance) {
                closest = zipline;
                from = zipline.getEnd();
                to = zipline.getStart();
                closestDistance = toEnd;
            }
        }

        if (closest != null) {
            start(player, closest, from, to);
        }
    }

    public void start(Player player, Zipline zipline, Location from, Location to) {
        rides.put(player.getUniqueId(), new ZiplineRide(seats, player, zipline, from, to));
        task.start();
    }

    /**
     * Ends a ride and releases the rider.
     *
     * @param completed whether the rider reached the far end, as opposed to bailing out or having
     *                  the zipline taken out from under them
     */
    public void stop(Player player, boolean completed) {
        ZiplineRide ride = rides.remove(player.getUniqueId());
        if (ride == null) {
            return;
        }

        ride.setEnding(true);
        ride.releaseSeat();

        triggerCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + TRIGGER_COOLDOWN_MILLIS);
        if (!ride.getZipline().getSettings().isFallDamage()) {
            fallGrace.put(player.getUniqueId(), System.currentTimeMillis() + FALL_GRACE_MILLIS);
        }

        if (player.isOnline()) {
            player.setVelocity(exitVelocity(ride, completed));
            player.setFallDistance(0);
            playEndSound(player, ride.getZipline().getSettings());
        }
        stopWhenIdle();
    }

    public void stopAll() {
        for (UUID uuid : Set.copyOf(rides.keySet())) {
            end(uuid, false);
        }
        stopWhenIdle();
    }

    public void stopRiders(Zipline zipline) {
        for (UUID uuid : Set.copyOf(rides.keySet())) {
            ZiplineRide ride = rides.get(uuid);
            if (ride != null && ride.getZipline() == zipline) {
                end(uuid, false);
            }
        }
        stopWhenIdle();
    }

    public void handleSneak(Player player) {
        ZiplineRide ride = rides.get(player.getUniqueId());
        if (ride != null && ride.getZipline().getSettings().isSneakExit()) {
            stop(player, false);
        }
    }

    /**
     * Decides what to do when a rider dismounts their seat.
     *
     * @return {@code true} if the dismount should be cancelled, which is how a rider is kept in
     * their seat on a zipline that does not allow bailing out
     */
    public boolean handleDismount(Player player, Entity vehicle) {
        ZiplineRide ride = rides.get(player.getUniqueId());
        if (ride == null || ride.isEnding() || !ride.isVehicle(vehicle)) {
            return false;
        }
        if (!ride.getZipline().getSettings().isSneakExit()) {
            return true;
        }
        stop(player, false);
        return false;
    }

    /**
     * Reports whether the player should be spared fall damage, consuming the grace period in the
     * process so that only the landing from the ride is covered.
     */
    public boolean isFallProtected(Player player) {
        if (isRiding(player)) {
            return true;
        }
        Long expiry = fallGrace.remove(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    /**
     * Drops everything remembered about a player, for when they quit or die mid-ride.
     */
    public void forget(Player player) {
        end(player.getUniqueId(), true);
        triggerCooldowns.remove(player.getUniqueId());
        fallGrace.remove(player.getUniqueId());
        stopWhenIdle();
    }

    /**
     * Removes a ride and releases its seat entities, optionally without touching the player.
     *
     * @param quiet whether to skip the exit velocity and sound, used when the player is gone or the
     *              ride is being torn down rather than finished
     */
    private void end(UUID uuid, boolean quiet) {
        Player player = quiet ? null : Bukkit.getPlayer(uuid);
        if (player != null) {
            stop(player, false);
            return;
        }

        ZiplineRide ride = rides.remove(uuid);
        if (ride != null) {
            ride.setEnding(true);
            ride.releaseSeat();
        }
    }

    private void playEndSound(Player player, ZiplineSettings settings) {
        Sound sound = settings.getEndSound();
        if (sound == null) {
            return;
        }
        player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS,
                (float) settings.getEndSoundVolume(), (float) settings.getEndSoundPitch());
    }

    private Vector exitVelocity(ZiplineRide ride, boolean completed) {
        if (!completed) {
            return new Vector(0, 0, 0);
        }
        ZiplineSettings settings = ride.getZipline().getSettings();
        if (settings.getExitMode() != ExitMode.LAUNCH) {
            return ride.getDirection().clone().multiply(settings.getBlocksPerTick() * DROP_CARRY);
        }

        Vector launch = ride.getDirection().clone().multiply(settings.getBlocksPerTick() * settings.getLaunchPower());
        launch.setY(Math.max(launch.getY(), launch.length() * LAUNCH_LIFT));
        return launch;
    }

    private boolean isOnCooldown(Player player) {
        Long expiry = triggerCooldowns.get(player.getUniqueId());
        if (expiry == null) {
            return false;
        }
        if (expiry > System.currentTimeMillis()) {
            return true;
        }
        triggerCooldowns.remove(player.getUniqueId());
        return false;
    }

    private void stopWhenIdle() {
        if (rides.isEmpty()) {
            task.stop();
        }
    }

    private void tickRides() {
        List<ZiplineRide> finished = new ArrayList<>();
        for (ZiplineRide ride : List.copyOf(rides.values())) {
            if (!ride.tick()) {
                finished.add(ride);
            }
        }
        for (ZiplineRide ride : finished) {
            stop(ride.getPlayer(), true);
        }
    }
}
