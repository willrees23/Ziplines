package com.github.willrees23.zipline;

import com.github.willrees23.zipline.path.PathGeometry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ZiplineIndex {

    private static final double CHUNK_SAMPLE_STEP = 2.0;

    private final Map<UUID, Map<Long, Set<Zipline>>> worlds = new HashMap<>();

    public void add(Zipline zipline) {
        World world = zipline.getStart().getWorld();
        if (world == null) {
            return;
        }
        Map<Long, Set<Zipline>> chunks = worlds.computeIfAbsent(world.getUID(), key -> new HashMap<>());
        for (long key : chunkKeys(zipline)) {
            chunks.computeIfAbsent(key, ignored -> new HashSet<>()).add(zipline);
        }
    }

    public void remove(Zipline zipline) {
        World world = zipline.getStart().getWorld();
        if (world == null) {
            return;
        }
        Map<Long, Set<Zipline>> chunks = worlds.get(world.getUID());
        if (chunks == null) {
            return;
        }
        for (long key : chunkKeys(zipline)) {
            Set<Zipline> bucket = chunks.get(key);
            if (bucket == null) {
                continue;
            }
            bucket.remove(zipline);
            if (bucket.isEmpty()) {
                chunks.remove(key);
            }
        }
    }

    public Set<Zipline> nearby(Location location, double radius) {
        Map<Long, Set<Zipline>> chunks = worlds.get(location.getWorld().getUID());
        if (chunks == null || chunks.isEmpty()) {
            return Set.of();
        }

        Set<Zipline> found = new HashSet<>();
        int minX = (int) Math.floor(location.getX() - radius) >> 4;
        int maxX = (int) Math.floor(location.getX() + radius) >> 4;
        int minZ = (int) Math.floor(location.getZ() - radius) >> 4;
        int maxZ = (int) Math.floor(location.getZ() + radius) >> 4;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Set<Zipline> bucket = chunks.get(key(x, z));
                if (bucket != null) {
                    found.addAll(bucket);
                }
            }
        }
        return found;
    }

    public void clear() {
        worlds.clear();
    }

    private Set<Long> chunkKeys(Zipline zipline) {
        Set<Long> keys = new HashSet<>();
        for (Vector point : PathGeometry.samplePoints(zipline.getStart(), zipline.getEnd(), CHUNK_SAMPLE_STEP)) {
            keys.add(key(point.getBlockX() >> 4, point.getBlockZ() >> 4));
        }
        return keys;
    }

    private long key(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}
