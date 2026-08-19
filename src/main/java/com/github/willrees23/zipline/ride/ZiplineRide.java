package com.github.willrees23.zipline.ride;

import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.path.PathGeometry;
import com.github.willrees23.zipline.seat.RideSeat;
import com.github.willrees23.zipline.seat.SeatFactory;
import com.github.willrees23.zipline.settings.MovementMode;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * One player's trip along one zipline.
 *
 * <p>Rides work one of two ways. A mounted ride puts the player on a seat entity that is driven
 * along the line, which is precise but visibly a vehicle. A velocity ride nudges the player towards
 * the next point on the line each tick, which leaves them in control of their own body but drifts,
 * so the correction is recomputed from their actual position every tick.
 */
@Getter
public class ZiplineRide {

    /**
     * Correction is capped at this multiple of ride speed, so a knock does not fling the rider.
     */
    private static final double MAX_CORRECTION = 2.0;

    /**
     * Floor for that cap, so a slow zipline can still pull a displaced rider back to the line.
     */
    private static final double MIN_CORRECTION = 0.5;

    private final Player player;
    private final Zipline zipline;
    private final Vector origin;
    private final Vector direction;
    private final double length;
    private final float yaw;
    private final RideSeat seat;

    @Setter
    private boolean ending;

    private double progress;
    private double travelled;
    private int ticks;

    public ZiplineRide(SeatFactory seats, Player player, Zipline zipline, Location from, Location to) {
        this.player = player;
        this.zipline = zipline;
        this.origin = from.toVector();

        Vector path = to.toVector().subtract(origin);
        this.length = path.length();
        this.direction = path.multiply(1 / this.length);
        this.yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));

        this.seat = zipline.getSettings().getMovementMode() == MovementMode.MOUNTED
                ? new RideSeat(seats, player, zipline.getSettings(), ridePoint(0))
                : null;
    }

    /**
     * Advances the ride by one tick, returning {@code false} once it should end.
     */
    public boolean tick() {
        if (!player.isOnline() || !player.getWorld().equals(zipline.getStart().getWorld())) {
            return false;
        }

        boolean running = seat == null ? tickVelocity() : tickMounted();
        if (!running) {
            return false;
        }

        playRideSound();
        ticks++;
        return true;
    }

    public boolean isVehicle(Entity entity) {
        return seat != null && seat.isVehicle(entity);
    }

    public void releaseSeat() {
        if (seat != null) {
            seat.release(player);
        }
    }

    /**
     * Distance travelled is tracked directly, since the seat goes exactly where it is told.
     */
    private boolean tickMounted() {
        if (!seat.isMounted(player)) {
            return false;
        }

        double speed = zipline.getSettings().getBlocksPerTick();
        travelled = Math.min(travelled + speed, length);
        progress = travelled / length;
        seat.move(player, ridePoint(travelled));
        player.setFallDistance(0);
        return travelled < length;
    }

    /**
     * Distance travelled is measured back from the player, by projecting them onto the line, because
     * their real position lags the velocity they were given.
     */
    private boolean tickVelocity() {
        double speed = zipline.getSettings().getBlocksPerTick();
        Vector position = player.getLocation().toVector();
        position.setY(PathGeometry.lineYFromRide(position.getY()));
        travelled = position.subtract(origin).dot(direction);
        progress = travelled / length;
        if (travelled >= length - speed) {
            return false;
        }

        Vector target = origin.clone().add(direction.clone().multiply(Math.min(travelled + speed, length)));
        target.setY(PathGeometry.velocityRideY(target.getY()));
        Vector velocity = target.subtract(player.getLocation().toVector());
        double maximum = Math.max(speed * MAX_CORRECTION, MIN_CORRECTION);
        if (velocity.length() > maximum) {
            velocity = velocity.normalize().multiply(maximum);
        }

        player.setVelocity(velocity);
        player.setFallDistance(0);
        return true;
    }

    /**
     * The point on the line at the given distance, dropped to ride height and turned to face along it.
     */
    private Location ridePoint(double distance) {
        Vector point = origin.clone().add(direction.clone().multiply(distance));
        return new Location(zipline.getStart().getWorld(),
                point.getX(),
                zipline.getProfile().rideHeight(point.getX(), point.getZ(), point.getY()),
                point.getZ(),
                yaw,
                0);
    }

    private void playRideSound() {
        ZiplineSettings settings = zipline.getSettings();
        Sound sound = settings.getRideSound();
        if (sound == null || ticks % settings.getRideSoundInterval() != 0) {
            return;
        }
        player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS,
                (float) settings.getRideSoundVolume(), (float) settings.ridePitch(progress));
    }
}
