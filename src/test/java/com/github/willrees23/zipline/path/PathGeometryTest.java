package com.github.willrees23.zipline.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathGeometryTest {

    /**
     * Locations are built without a world, which the geometry never looks at.
     */
    private static Location at(double x, double y, double z) {
        return new Location(null, x, y, z);
    }

    @Test
    @DisplayName("a straight run along one axis visits every block once")
    void straightRunVisitsEveryBlock() {
        List<Vector> blocks = PathGeometry.pathBlocks(at(0.5, 64.5, 0.5), at(10.5, 64.5, 0.5));

        assertEquals(11, blocks.size());
        for (int index = 0; index < blocks.size(); index++) {
            assertEquals(index, blocks.get(index).getBlockX());
            assertEquals(0, blocks.get(index).getBlockZ());
        }
    }

    @Test
    @DisplayName("the line is strung above the marked endpoints")
    void lineIsRaisedAboveTheEndpoints() {
        List<Vector> blocks = PathGeometry.pathBlocks(at(0.5, 64.5, 0.5), at(4.5, 64.5, 0.5));

        for (Vector block : blocks) {
            assertEquals(64 + PathGeometry.PATH_RISE, block.getBlockY());
        }
    }

    @Test
    @DisplayName("a diagonal run steps one axis at a time, with no gaps or repeats")
    void diagonalRunIsContiguous() {
        List<Vector> blocks = PathGeometry.pathBlocks(at(0.5, 64.5, 0.5), at(12.5, 71.5, -9.5));

        Set<Vector> seen = new HashSet<>();
        for (Vector block : blocks) {
            assertTrue(seen.add(block), "visited " + block + " twice");
        }

        for (int index = 1; index < blocks.size(); index++) {
            Vector previous = blocks.get(index - 1);
            Vector current = blocks.get(index);
            int moved = Math.abs(current.getBlockX() - previous.getBlockX())
                    + Math.abs(current.getBlockY() - previous.getBlockY())
                    + Math.abs(current.getBlockZ() - previous.getBlockZ());
            assertEquals(1, moved, "jumped from " + previous + " to " + current);
        }
    }

    @Test
    @DisplayName("the walk starts and finishes on the endpoint blocks")
    void walkCoversBothEndpoints() {
        Location start = at(3.2, 64.8, -7.1);
        Location end = at(-11.9, 70.2, 4.6);
        List<Vector> blocks = PathGeometry.pathBlocks(start, end);

        Vector first = blocks.get(0);
        Vector last = blocks.get(blocks.size() - 1);
        assertEquals(start.getBlockX(), first.getBlockX());
        assertEquals(start.getBlockZ(), first.getBlockZ());
        assertEquals(end.getBlockX(), last.getBlockX());
        assertEquals(end.getBlockZ(), last.getBlockZ());
    }

    @Test
    @DisplayName("two endpoints in the same block produce a single block")
    void sameBlockProducesOneBlock() {
        List<Vector> blocks = PathGeometry.pathBlocks(at(0.1, 64.1, 0.1), at(0.9, 64.9, 0.9));

        assertEquals(1, blocks.size());
    }

    @Test
    @DisplayName("sampling covers the whole line and finishes exactly on the end")
    void samplingReachesTheEnd() {
        Location start = at(0, 64, 0);
        Location end = at(10, 64, 0);
        List<Vector> points = PathGeometry.samplePoints(start, end, 2.0);

        assertEquals(new Vector(0, 64, 0), points.get(0));
        assertEquals(end.toVector(), points.get(points.size() - 1));
        for (Vector point : points) {
            assertTrue(point.getX() >= 0 && point.getX() <= 10, "sampled off the line at " + point);
        }
    }

    @Test
    @DisplayName("sampling a zero length line yields the point itself")
    void samplingZeroLengthLine() {
        List<Vector> points = PathGeometry.samplePoints(at(1, 2, 3), at(1, 2, 3), 0.5);

        assertEquals(List.of(new Vector(1, 2, 3)), points);
    }

    @Test
    @DisplayName("the packed keys cover exactly the blocks the walk visits")
    void pathBlockKeysMatchTheWalk() {
        Location start = at(3.2, 64.8, -7.1);
        Location end = at(-11.9, 70.2, 4.6);

        List<Vector> blocks = PathGeometry.pathBlocks(start, end);
        Set<Long> keys = PathGeometry.pathBlockKeys(start, end);

        assertEquals(blocks.size(), keys.size());
        for (Vector block : blocks) {
            assertTrue(keys.contains(
                            PathGeometry.blockKey(block.getBlockX(), block.getBlockY(), block.getBlockZ())),
                    "missing " + block);
        }
    }

    @Test
    @DisplayName("a block just off the line is not one of its keys")
    void pathBlockKeysExcludeBlocksOffTheLine() {
        Set<Long> keys = PathGeometry.pathBlockKeys(at(0.5, 64.5, 0.5), at(10.5, 64.5, 0.5));

        int lineY = 64 + PathGeometry.PATH_RISE;
        assertTrue(keys.contains(PathGeometry.blockKey(5, lineY, 0)));
        assertFalse(keys.contains(PathGeometry.blockKey(5, lineY, 1)), "the block beside the line");
        assertFalse(keys.contains(PathGeometry.blockKey(5, lineY - 1, 0)), "the block below the line");
        assertFalse(keys.contains(PathGeometry.blockKey(11, lineY, 0)), "the block past the end");
    }

    @Test
    @DisplayName("distinct block positions pack to distinct keys, negatives and world edges included")
    void blockKeysAreDistinct() {
        Set<Long> keys = new HashSet<>();
        int[] coordinates = {-30_000_000, -4096, -1, 0, 1, 4096, 30_000_000};

        int packed = 0;
        for (int x : coordinates) {
            for (int y : new int[]{-64, 0, 319}) {
                for (int z : coordinates) {
                    keys.add(PathGeometry.blockKey(x, y, z));
                    packed++;
                }
            }
        }

        assertEquals(packed, keys.size(), "two positions packed to the same key");
    }

    @Test
    @DisplayName("ride height converts back to the height of the line")
    void velocityRideHeightRoundTrips() {
        assertEquals(70.0, PathGeometry.lineYFromRide(PathGeometry.velocityRideY(70.0)));
    }

    @Test
    @DisplayName("clearance covers the rider, their seat and the line itself")
    void clearanceCoversTheWholeRide() {
        assertEquals(1 + PathGeometry.MOUNT_DROP + PathGeometry.VEHICLE_DEPTH, PathGeometry.CLEARANCE_DEPTH);
    }

    @Test
    @DisplayName("a point part way along the line reads back as that fraction of it")
    void fractionAlongMeasuresProgress() {
        Location from = at(0, 64, 0);
        Location to = at(10, 64, 0);

        assertEquals(0.0, PathGeometry.fractionAlong(from, to, at(0, 64, 0)));
        assertEquals(0.25, PathGeometry.fractionAlong(from, to, at(2.5, 64, 0)));
        assertEquals(1.0, PathGeometry.fractionAlong(from, to, at(10, 64, 0)));
    }

    @Test
    @DisplayName("height is left out of the measurement, so a seat hanging below the line still counts")
    void fractionAlongIgnoresHeight() {
        Location from = at(0, 64, 0);
        Location to = at(8, 80, 6);

        assertEquals(0.5, PathGeometry.fractionAlong(from, to, at(4, 64, 3)));
    }

    @Test
    @DisplayName("a point beyond either end counts as being at that end")
    void fractionAlongIsClampedToTheLine() {
        Location from = at(0, 64, 0);
        Location to = at(10, 64, 0);

        assertEquals(0.0, PathGeometry.fractionAlong(from, to, at(-5, 64, 0)));
        assertEquals(1.0, PathGeometry.fractionAlong(from, to, at(25, 64, 0)));
    }

    @Test
    @DisplayName("a line with no length reads as being at its start rather than dividing by zero")
    void fractionAlongHandlesAPointLine() {
        Location point = at(3, 64, 3);

        assertEquals(0.0, PathGeometry.fractionAlong(point, point, at(9, 64, 9)));
    }
}
