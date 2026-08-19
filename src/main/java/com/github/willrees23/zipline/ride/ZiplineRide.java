package com.github.willrees23.zipline.ride;

import com.github.willrees23.enums.MovementMode;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineSettings;
import com.github.willrees23.zipline.path.PathGeometry;
import com.github.willrees23.zipline.seat.RideSeat;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@Getter
public class ZiplineRide {

    private static final double MAX_CORRECTION = 2.0;
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

    public ZiplineRide(Player player, Zipline zipline, Location from, Location to) {
        this.player = player;
        this.zipline = zipline;
        this.origin = from.toVector();
        Vector path = to.toVector().subtract(origin);
        this.length = path.length();
        this.direction = path.multiply(1 / this.length);
        this.yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        this.seat = zipline.getSettings().getMovementMode() == MovementMode.MOUNTED
                ? new RideSeat(player, zipline.getSettings(), ridePoint(0))
                : null;
    }

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
