package com.github.willrees23.zipline.path;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.List;

/** Checks that a proposed line has room for a rider to pass beneath it. */
@UtilityClass
public class PathClearance {

    /**
     * Returns the first solid block found in the space a rider would occupy, or {@code null} when
     * the whole line is clear. Positions outside the world height limits count as obstructed.
     */
    public Location firstObstruction(World world, List<Vector> pathBlocks) {
        for (Vector position : pathBlocks) {
            for (int depth = 0; depth < PathGeometry.CLEARANCE_DEPTH; depth++) {
                int y = position.getBlockY() - depth;
                if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                    return new Location(world, position.getBlockX(), y, position.getBlockZ());
                }
                Block block = world.getBlockAt(position.getBlockX(), y, position.getBlockZ());
                if (block.getType().isSolid()) {
                    return block.getLocation();
                }
            }
        }
        return null;
    }
}
