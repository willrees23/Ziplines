package com.github.willrees23.zipline.path;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("ride height converts back to the height of the line")
    void velocityRideHeightRoundTrips() {
        assertEquals(70.0, PathGeometry.lineYFromRide(PathGeometry.velocityRideY(70.0)));
    }

    @Test
    @DisplayName("clearance covers the rider, their seat and the line itself")
    void clearanceCoversTheWholeRide() {
        assertEquals(1 + PathGeometry.MOUNT_DROP + PathGeometry.VEHICLE_DEPTH, PathGeometry.CLEARANCE_DEPTH);
    }
}
