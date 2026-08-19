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

public class BlockPathRenderer implements PathRenderer {

    private static final String SEPARATOR = ";";

    @Override
    public int apply(Zipline zipline) {
        World world = zipline.getStart().getWorld();
        BlockData data = zipline.getSettings().getMaterial().createBlockData();
        List<String> snapshot = new ArrayList<>();
        int replaced = 0;

        for (Vector position : PathGeometry.pathBlocks(zipline.getStart(), zipline.getEnd())) {
            Block block = world.getBlockAt(position.getBlockX(), position.getBlockY(), position.getBlockZ());
            if (block.getType() == zipline.getSettings().getMaterial()) {
                continue;
            }
            if (block.getType() != Material.AIR) {
                replaced++;
            }
            snapshot.add(serialize(position, block.getBlockData().getAsString()));
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

        List<String> snapshot = zipline.getPlacedBlocks();
        for (int index = snapshot.size() - 1; index >= 0; index--) {
            String[] parts = snapshot.get(index).split(SEPARATOR, 4);
            if (parts.length < 4) {
                continue;
            }
            try {
                Block block = world.getBlockAt(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                block.setBlockData(Bukkit.createBlockData(parts[3]), true);
            } catch (IllegalArgumentException exception) {
                continue;
            }
        }
        zipline.setPlacedBlocks(new ArrayList<>());
    }

    @Override
    public void tick(Zipline zipline, Player viewer) {
    }

    private String serialize(Vector position, String data) {
        return position.getBlockX() + SEPARATOR + position.getBlockY() + SEPARATOR + position.getBlockZ() + SEPARATOR + data;
    }
}
