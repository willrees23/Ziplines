package com.github.willrees23.zipline.path;

import com.github.willrees23.zipline.Zipline;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/** Draws the line as real blocks, remembering what it replaced so that the change can be undone. */
public class BlockPathRenderer implements PathRenderer {

    @Override
    public int apply(Zipline zipline) {
        World world = zipline.getStart().getWorld();
        BlockData data = zipline.getSettings().getMaterial().createBlockData();
        List<PlacedBlock> snapshot = new ArrayList<>();
        int replaced = 0;

        for (Vector position : PathGeometry.pathBlocks(zipline.getStart(), zipline.getEnd())) {
            Block block = world.getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
            if (block.getType() == zipline.getSettings().getMaterial()) {
                continue;
            }
            if (block.getType() != Material.AIR) {
                replaced++;
            }
            snapshot.add(new PlacedBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ(),
                    block.getBlockData().getAsString()));
            block.setBlockData(data, true);
        }

        zipline.setPlacedBlocks(snapshot);
        return replaced;
    }

    @Override
    public void remove(Zipline zipline) {
        World world = zipline.getStart().getWorld();
        if (world == null) {
            return;
        }

        // Restored back to front, so that a block placed over an earlier one is undone first.
        List<PlacedBlock> snapshot = zipline.getPlacedBlocks();
        for (int index = snapshot.size() - 1; index >= 0; index--) {
            PlacedBlock placed = snapshot.get(index);
            try {
                world.getBlockAt(placed.x(), placed.y(), placed.z())
                        .setBlockData(Bukkit.createBlockData(placed.data()), true);
            } catch (IllegalArgumentException exception) {
                // Block data from an older server version that no longer parses; leave it as it is.
            }
        }
        zipline.setPlacedBlocks(List.of());
    }

    @Override
    public void tick(Zipline zipline, Player viewer) {
        // Nothing to do: the blocks are already in the world.
    }
}
