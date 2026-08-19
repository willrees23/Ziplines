package com.github.willrees23.zipline.path;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PathGeometry {

    public final int PATH_RISE = 2;
    public final int MOUNT_DROP = 2;
    public final int VELOCITY_DROP = 1;
    public final int VEHICLE_DEPTH = 1;
    public final int CLEARANCE_DEPTH = 1 + MOUNT_DROP + VEHICLE_DEPTH;

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

    public int pathBlockY(double lineY) {
        return (int) Math.floor(lineY) + PATH_RISE;
    }

    public double velocityRideY(double lineY) {
        return lineY - VELOCITY_DROP;
    }

    public double lineYFromRide(double rideY) {
        return rideY + VELOCITY_DROP;
    }

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
