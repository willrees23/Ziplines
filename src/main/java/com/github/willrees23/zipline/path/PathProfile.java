package com.github.willrees23.zipline.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

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

    public double rideHeight(double x, double z, double lineY) {
        Integer lowest = lowestByColumn.get(key((int) Math.floor(x), (int) Math.floor(z)));
        if (lowest == null) {
            return PathGeometry.steppedRideY(lineY);
        }
        return lowest - PathGeometry.MOUNT_DROP;
    }

    private long key(int blockX, int blockZ) {
        return ((long) blockX << 32) ^ (blockZ & 0xffffffffL);
    }
}
