package com.github.willrees23.zipline.ride;

import com.github.willrees23.ZiplinesPlugin;
import com.github.willrees23.enums.ExitMode;
import com.github.willrees23.enums.TriggerMode;
import com.github.willrees23.enums.ZiplinePermission;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.ZiplineSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RideManager {

    private static final long RIDE_INTERVAL = 1L;
    private static final long TRIGGER_COOLDOWN_MILLIS = 1000L;
    private static final long FALL_GRACE_MILLIS = 10000L;
    private static final double DROP_CARRY = 0.5;
    private static final double LAUNCH_LIFT = 0.35;

    private static RideManager instance;

    private final Map<UUID, ZiplineRide> rides = new HashMap<>();
    private final Map<UUID, Long> triggerCooldowns = new HashMap<>();
    private final Map<UUID, Long> fallGrace = new HashMap<>();

    private BukkitTask rideTask;

    public static RideManager getInstance() {
        if (instance == null) {
            instance = new RideManager();
        }
        return instance;
    }

    public boolean isRiding(Player player) {
        return rides.containsKey(player.getUniqueId());
    }

    public void tryStart(Player player, TriggerMode mode) {
        if (isRiding(player) || isOnCooldown(player) || !player.hasPermission(ZiplinePermission.ZIPLINE_USE.getPermission())) {
            return;
        }

        double radius = ZiplineManager.getInstance().getConfiguration().getTriggerRadius();
        Location eyes = player.getEyeLocation();
        Set<Zipline> candidates = ZiplineManager.getInstance().getIndex().nearby(eyes, radius);

        Zipline closest = null;
        Location from = null;
        Location to = null;
        double closestDistance = radius * radius;
        for (Zipline zipline : candidates) {
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

        if (closest == null) {
            return;
        }
        start(player, closest, from, to);
    }

    public void start(Player player, Zipline zipline, Location from, Location to) {
        rides.put(player.getUniqueId(), new ZiplineRide(player, zipline, from, to));
        startRideTask();
    }

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
        stopRideTask();
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

    public void stopAll() {
        for (UUID uuid : Set.copyOf(rides.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stop(player, false);
            } else {
                discard(uuid);
            }
        }
        stopRideTask();
    }

    public void stopRiders(Zipline zipline) {
        for (UUID uuid : Set.copyOf(rides.keySet())) {
            ZiplineRide ride = rides.get(uuid);
            if (ride == null || ride.getZipline() != zipline) {
                continue;
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stop(player, false);
            } else {
                discard(uuid);
            }
        }
    }

    public void handleSneak(Player player) {
        ZiplineRide ride = rides.get(player.getUniqueId());
        if (ride == null || !ride.getZipline().getSettings().isSneakExit()) {
            return;
        }
        stop(player, false);
    }

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

    public boolean isFallProtected(Player player) {
        if (isRiding(player)) {
            return true;
        }
        Long expiry = fallGrace.get(player.getUniqueId());
        if (expiry == null) {
            return false;
        }
        fallGrace.remove(player.getUniqueId());
        return expiry > System.currentTimeMillis();
    }

    public void forget(Player player) {
        discard(player.getUniqueId());
        triggerCooldowns.remove(player.getUniqueId());
        fallGrace.remove(player.getUniqueId());
        stopRideTask();
    }

    private void discard(UUID uuid) {
        ZiplineRide ride = rides.remove(uuid);
        if (ride == null) {
            return;
        }
        ride.setEnding(true);
        ride.releaseSeat();
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

    private void startRideTask() {
        if (rideTask != null) {
            return;
        }
        rideTask = Bukkit.getScheduler().runTaskTimer(ZiplinesPlugin.getInstance(), this::tickRides, 0L, RIDE_INTERVAL);
    }

    private void stopRideTask() {
        if (rideTask == null || !rides.isEmpty()) {
            return;
        }
        rideTask.cancel();
        rideTask = null;
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
