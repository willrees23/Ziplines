package com.github.willrees23.zipline.seat;

import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.path.PathGeometry;
import com.github.willrees23.zipline.path.PathProfile;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Vector;

/**
 * One seat making its own way back to the end it belongs to.
 *
 * <p>The trip retraces the ride that carried the seat off: the same line, at the same speed, so
 * that crossing a long line leaves the next player waiting as long as the crossing itself took. A
 * seat abandoned part way along has only that much of the line to cover.
 *
 * <p>Nothing is riding the seat on the way back, so the display is teleported along the line rather
 * than driven by a vehicle. It keeps the facing it parks with throughout, and so slides home
 * backwards rather than turning round at each end.
 */
final class SeatReturn {

    /**
     * Ticks the client is told to spread each hop over, matching how often they arrive, so that the
     * seat glides rather than stepping between teleports.
     */
    private static final int SMOOTHING = 1;

    private final BlockDisplay display;
    private final PathProfile profile;
    private final World world;
    private final Location home;
    private final Vector origin;
    private final Vector direction;
    private final double length;
    private final double step;

    private double travelled;

    /**
     * @param from    the end of the line the seat is coming back from
     * @param to      the end it belongs to
     * @param home    where it parks at that end, which is where it is put down on arrival
     */
    SeatReturn(Zipline zipline, BlockDisplay display, Location from, Location to, Location home) {
        this.display = display;
        this.profile = zipline.getProfile();
        this.world = home.getWorld();
        this.home = home;
        this.origin = from.toVector();

        Vector path = to.toVector().subtract(origin);
        this.length = path.length();
        this.direction = path.multiply(1 / this.length);
        this.step = zipline.getSettings().getBlocksPerTick();
        this.travelled = PathGeometry.fractionAlong(from, to, display.getLocation()) * this.length;

        display.setTeleportDuration(SMOOTHING);
    }

    /**
     * Moves the seat one tick further along, returning {@code false} once it is home or the display
     * has not survived the trip.
     */
    boolean tick() {
        if (!display.isValid()) {
            return false;
        }

        travelled = Math.min(travelled + step, length);
        if (travelled >= length) {
            park();
            return false;
        }

        display.teleport(point(travelled));
        return true;
    }

    /**
     * Puts the seat down where it belongs, and stops the client smoothing its moves, so that a seat
     * lent out to the next ride takes up its place on the vehicle at once.
     */
    private void park() {
        display.setTeleportDuration(0);
        display.teleport(home);
    }

    /**
     * The point on the line at the given distance, dropped to ride height so that the seat comes
     * back the way it went out.
     */
    private Location point(double distance) {
        Vector position = origin.clone().add(direction.clone().multiply(distance));
        return new Location(world,
                position.getX(),
                profile.rideHeight(position.getX(), position.getZ(), position.getY()),
                position.getZ(),
                home.getYaw(),
                home.getPitch());
    }
}
