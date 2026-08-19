package com.github.willrees23.zipline.path;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * The shared geometry of a zipline: where its line of blocks sits relative to the two points that
 * were marked out, and where a rider sits relative to that line.
 *
 * <p>Endpoints are marked at eye level, but the line itself is strung overhead and riders hang
 * beneath it. Every offset involved lives here so that the renderer, the ride and the clearance
 * check cannot drift apart.
 */
@UtilityClass
public class PathGeometry {

    /**
     * How far above the marked endpoints the line of blocks is strung.
     */
    public final int PATH_RISE = 2;
    /**
     * How far below the line a mounted rider hangs.
     */
    public final int MOUNT_DROP = 2;
    /**
     * How far below the line a rider travelling under their own velocity sits.
     */
    public final int VELOCITY_DROP = 1;
    /**
     * How much room the seat entity needs beneath the rider.
     */
    public final int VEHICLE_DEPTH = 1;
    /**
     * Blocks that must be clear at and below each block of the line for a ride to fit through.
     */
    public final int CLEARANCE_DEPTH = 1 + MOUNT_DROP + VEHICLE_DEPTH;

    /**
     * Returns points spaced {@code step} apart along the straight line between the endpoints.
     */
    public List<Vector> samplePoints(Location start, Location end, double step) {
        List<Vector> points = new ArrayList<>();
        Vector origin = start.toVector();
        Vector direction = end.toVector().subtract(origin);
        double length = direction.length();
        if (length <= 0) {
            points.add(origin);
            return points;
        }

        Vector unit = direction.multiply(1 / length);
        for (double travelled = 0; travelled < length; travelled += step) {
            points.add(origin.clone().add(unit.clone().multiply(travelled)));
        }
        points.add(end.toVector());
        return points;
    }

    /**
     * Returns every block the line passes through, in order, already raised by {@link #PATH_RISE}.
     *
     * <p>This is a voxel walk rather than a sampled line: sampling at a fixed step either skips
     * blocks on a shallow diagonal or visits the same block several times, and the block renderer
     * needs each one exactly once so that it can record what it replaced.
     *
     * <p>Each {@code next} value holds how far along the line, as a fraction of its full length, the
     * next boundary crossing on that axis lies, and each {@code span} value holds how much that
     * advances per block. Every iteration steps whichever axis has the nearest crossing, which is
     * the order the line crosses them. An axis that has reached its target is parked at infinity so
     * that it is never chosen again.
     */
    public List<Vector> pathBlocks(Location start, Location end) {
        Vector origin = start.toVector();
        Vector delta = end.toVector().subtract(origin);

        int x = start.getBlockX();
        int y = start.getBlockY();
        int z = start.getBlockZ();
        int targetX = end.getBlockX();
        int targetY = end.getBlockY();
        int targetZ = end.getBlockZ();

        int stepX = axisStep(delta.getX());
        int stepY = axisStep(delta.getY());
        int stepZ = axisStep(delta.getZ());

        double spanX = axisSpan(delta.getX());
        double spanY = axisSpan(delta.getY());
        double spanZ = axisSpan(delta.getZ());

        double nextX = x == targetX ? Double.POSITIVE_INFINITY : axisBoundary(origin.getX(), x, stepX, delta.getX());
        double nextY = y == targetY ? Double.POSITIVE_INFINITY : axisBoundary(origin.getY(), y, stepY, delta.getY());
        double nextZ = z == targetZ ? Double.POSITIVE_INFINITY : axisBoundary(origin.getZ(), z, stepZ, delta.getZ());

        int steps = Math.abs(targetX - x) + Math.abs(targetY - y) + Math.abs(targetZ - z);
        List<Vector> blocks = new ArrayList<>(steps + 1);
        blocks.add(new Vector(x, y + PATH_RISE, z));

        for (int taken = 0; taken < steps; taken++) {
            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX;
                nextX = x == targetX ? Double.POSITIVE_INFINITY : nextX + spanX;
            } else if (nextY <= nextZ) {
                y += stepY;
                nextY = y == targetY ? Double.POSITIVE_INFINITY : nextY + spanY;
            } else {
                z += stepZ;
                nextZ = z == targetZ ? Double.POSITIVE_INFINITY : nextZ + spanZ;
            }
            blocks.add(new Vector(x, y + PATH_RISE, z));
        }
        return blocks;
    }

    /**
     * Returns the block of the line strung above the given height.
     */
    public int pathBlockY(double lineY) {
        return (int) Math.floor(lineY) + PATH_RISE;
    }

    public double velocityRideY(double lineY) {
        return lineY - VELOCITY_DROP;
    }

    /**
     * Inverse of {@link #velocityRideY}, for recovering the line from where a rider currently is.
     */
    public double lineYFromRide(double rideY) {
        return rideY + VELOCITY_DROP;
    }

    /**
     * Ride height for a mounted rider, snapped to the block of the line above them.
     */
    public double steppedRideY(double lineY) {
        return pathBlockY(lineY) - MOUNT_DROP;
    }

    private int axisStep(double delta) {
        if (delta > 0) {
            return 1;
        }
        return delta < 0 ? -1 : 0;
    }

    private double axisSpan(double delta) {
        return delta == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / delta);
    }

    private double axisBoundary(double origin, int block, int step, double delta) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = step > 0 ? block + 1 : block;
        return (boundary - origin) / delta;
    }
}
