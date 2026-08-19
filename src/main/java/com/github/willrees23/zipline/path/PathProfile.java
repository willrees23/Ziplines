package com.github.willrees23.zipline.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

/**
 * The ride height of a zipline, worked out once per column of blocks.
 *
 * <p>A line climbing at a shallow angle passes through several blocks stacked in the same column.
 * A rider crossing that column has to hang below the lowest of them, otherwise they clip up into
 * the line on the way past. That lowest block is found once, when the zipline gets its endpoints,
 * and then reused for every tick of every ride.
 */
public class PathProfile {

    private final Map<Long, Integer> lowestByColumn = new HashMap<>();

    public PathProfile(Location start, Location end) {
        for (Vector block : PathGeometry.pathBlocks(start, end)) {
            long key = key(block.getBlockX(), block.getBlockZ());
            Integer lowest = lowestByColumn.get(key);
            if (lowest == null || block.getBlockY() < lowest) {
                lowestByColumn.put(key, block.getBlockY());
            }
        }
    }

    /**
     * Returns the height a rider should sit at while crossing the given column, falling back to the
     * line itself for a column the path does not cover.
     */
    public double rideHeight(double x, double z, double lineY) {
        Integer lowest = lowestByColumn.get(key((int) Math.floor(x), (int) Math.floor(z)));
        if (lowest == null) {
            return PathGeometry.steppedRideY(lineY);
        }
        return lowest - PathGeometry.MOUNT_DROP;
    }

    /**
     * Packs a block column into one long, so the map needs no object key per column.
     */
    private long key(int blockX, int blockZ) {
        return ((long) blockX << 32) ^ (blockZ & 0xffffffffL);
    }
}
