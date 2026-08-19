package com.github.willrees23.zipline.path;

import com.github.willrees23.zipline.Zipline;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Draws the line with particles, leaving the world untouched. */
public class ParticlePathRenderer implements PathRenderer {

    private static final double PARTICLE_STEP = 0.5;
    private static final double VIEW_DISTANCE_SQUARED = 32 * 32;

    @Override
    public int apply(Zipline zipline) {
        return 0;
    }

    @Override
    public void remove(Zipline zipline) {
        // Nothing was placed, so there is nothing to restore.
    }

    @Override
    public void tick(Zipline zipline, Player viewer) {
        Location point = zipline.getStart().clone();
        Vector viewerPosition = viewer.getLocation().toVector();
        for (Vector position : PathGeometry.samplePoints(zipline.getStart(), zipline.getEnd(), PARTICLE_STEP)) {
            if (position.distanceSquared(viewerPosition) > VIEW_DISTANCE_SQUARED) {
                continue;
            }
            point.setX(position.getX());
            point.setY(position.getY() + PathGeometry.PATH_RISE);
            point.setZ(position.getZ());
            viewer.spawnParticle(zipline.getSettings().getPathParticle(), point, 1, 0, 0, 0, 0);
        }
    }
}
