package com.github.willrees23.zipline.path;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.List;

@UtilityClass
public class PathClearance {

    public final int REQUIRED_HEIGHT = PathGeometry.CLEARANCE_DEPTH;

    public Location firstObstruction(World world, List<Vector> pathBlocks) {
        for (Vector position : pathBlocks) {
            for (int depth = 0; depth < REQUIRED_HEIGHT; depth++) {
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
