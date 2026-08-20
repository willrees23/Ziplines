package com.github.willrees23.zipline.ride;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.config.ZiplineConfig;
import com.github.willrees23.task.RepeatingTask;
import com.github.willrees23.util.ChatUtil;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineIndex;
import com.github.willrees23.zipline.seat.SeatFactory;
import com.github.willrees23.zipline.seat.SeatManager;
import com.github.willrees23.zipline.settings.ExitMode;
import com.github.willrees23.zipline.settings.RideDirection;
import com.github.willrees23.zipline.settings.TriggerMode;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.BlockDisplay;
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
     * How long a player turned away from a line waits before being told again, so that milling
     * about at a busy endpoint does not fill their chat.
     */
    private static final long BUSY_NOTICE_MILLIS = 3000L;

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
    private final SeatManager endpointSeats;

    private final Map<UUID, ZiplineRide> rides = new HashMap<>();
    private final Map<UUID, Long> triggerCooldowns = new HashMap<>();
    private final Map<UUID, Long> fallGrace = new HashMap<>();
    private final Map<UUID, Long> busyNotices = new HashMap<>();

    private final RepeatingTask task;

    public RideManager(Plugin plugin,
                       ZiplineConfig config,
                       ZiplineIndex index,
                       SeatFactory seats,
                       SeatManager endpointSeats) {
        this.config = config;
        this.index = index;
        this.seats = seats;
        this.endpointSeats = endpointSeats;
        this.task = new RepeatingTask(plugin, RIDE_INTERVAL, this::tickRides);
    }

    public boolean isRiding(Player player) {
        return rides.containsKey(player.getUniqueId());
    }

    /**
     * Starts a ride if the player is stood at the end of a zipline that uses the given trigger.
     *
     * <p>Every end a candidate can be boarded from is considered, and the nearest wins, so a line
     * is ridden in whichever direction the player walked up to it, unless its {@code direction}
     * setting only allows one of them. A line already carrying as many riders as its
     * {@code max-riders} setting allows, or waiting on its seat to come back, is passed over, so a
     * player stood between two lines still boards the one that is free.
     */
    public void tryStart(Player player, TriggerMode mode) {
        if (isRiding(player) || isOnCooldown(player)
                || !player.hasPermission(ZiplinePermission.ZIPLINE_USE.getNode())) {
            return;
        }

        double radius = config.getTriggerRadius();
        Location eyes = player.getEyeLocation();

        Zipline closest = null;
        Zipline busy = null;
        Location from = null;
        Location to = null;
        double closestDistance = radius * radius;
        for (Zipline zipline : index.nearby(eyes, radius)) {
            if (zipline.getSettings().getTrigger() != mode) {
                continue;
            }

            RideDirection direction = zipline.getSettings().getDirection();
            double toStart = direction.allowsStart() ? zipline.getStart().distanceSquared(eyes) : Double.MAX_VALUE;
            double toEnd = direction.allowsEnd() ? zipline.getEnd().distanceSquared(eyes) : Double.MAX_VALUE;
            double nearest = Math.min(toStart, toEnd);
            if (nearest >= closestDistance) {
                continue;
            }

            if (isBusy(zipline)) {
                // Remembered rather than acted on, in case a line with room turns out to be nearer.
                busy = zipline;
                continue;
            }

            boolean fromStart = toStart <= toEnd;
            closest = zipline;
            from = fromStart ? zipline.getStart() : zipline.getEnd();
            to = fromStart ? zipline.getEnd() : zipline.getStart();
            closestDistance = nearest;
        }

        if (closest != null) {
            start(player, closest, from, to);
        } else if (busy != null) {
            notifyBusy(player, busy);
        }
    }

    public void start(Player player, Zipline zipline, Location from, Location to) {
        // A single rider line rides the seat already parked at the end they board from, so that the
        // seat they walked up to is the one that carries them rather than a second one on top of it.
        BlockDisplay endpointSeat = zipline.getSettings().carriesEndpointSeat()
                ? endpointSeats.lend(zipline, from)
                : null;
        rides.put(player.getUniqueId(), new ZiplineRide(seats, player, zipline, from, to, endpointSeat));
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
        releaseSeat(ride);

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
        busyNotices.remove(player.getUniqueId());
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
            releaseSeat(ride);
        }
    }

    /**
     * Lets go of the ride's seat, parking it back at its end if it was the endpoint's own.
     */
    private void releaseSeat(ZiplineRide ride) {
        boolean borrowed = ride.isCarryingEndpointSeat();
        ride.releaseSeat();
        if (borrowed) {
            endpointSeats.restore(ride.getZipline());
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

    /**
     * Tells the player why they were turned away, at most once every {@link #BUSY_NOTICE_MILLIS},
     * since the walk trigger fires again on every block they cross.
     */
    private void notifyBusy(Player player, Zipline zipline) {
        long now = System.currentTimeMillis();
        Long expiry = busyNotices.get(player.getUniqueId());
        if (expiry != null && expiry > now) {
            return;
        }

        busyNotices.put(player.getUniqueId(), now + BUSY_NOTICE_MILLIS);
        if (isFull(zipline)) {
            ChatUtil.sendHighlighted(player, "&cZipline %s is full: it carries %s at a time.",
                    zipline.getId(), riders(zipline.getSettings().getMaxRiders()));
        } else {
            ChatUtil.sendHighlighted(player, "&cZipline %s is not ready: wait for its seat to come back.",
                    zipline.getId());
        }
    }

    private String riders(int limit) {
        return limit == 1 ? "one rider" : limit + " riders";
    }

    /**
     * Whether the zipline cannot take another rider at the moment, either because it is carrying
     * all it is allowed to or because the seat to board is still on its way back.
     */
    private boolean isBusy(Zipline zipline) {
        return isFull(zipline) || endpointSeats.isReturning(zipline);
    }

    /**
     * Whether the zipline is already carrying as many riders as it is allowed to.
     */
    private boolean isFull(Zipline zipline) {
        return !zipline.getSettings().allowsRider(riderCount(zipline));
    }

    private int riderCount(Zipline zipline) {
        int riding = 0;
        for (ZiplineRide ride : rides.values()) {
            if (ride.getZipline() == zipline) {
                riding++;
            }
        }
        return riding;
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
