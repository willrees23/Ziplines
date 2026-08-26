package com.github.willrees23.zipline;

import com.github.willrees23.zipline.path.PathType;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import org.bukkit.block.Block;

/**
 * Finds the zipline a block belongs to, for lines that are holding their path together.
 *
 * <p>Only a line drawn with blocks has anything in the world to protect. A particle line puts
 * nothing there, so whatever happens to stand where one runs belongs to whoever built it.
 */
public class ZiplineProtection {

    /**
     * How far around a block to look for ziplines.
     *
     * <p>A chunk either way. The index files a line under the chunks a sampled walk of it lands in,
     * so a line clipping the corner of a chunk between two samples may only be filed under its
     * neighbours, and asking for the one chunk the block sits in would miss it.
     */
    private static final double SEARCH_RADIUS = 16.0;

    private final ZiplineIndex index;

    public ZiplineProtection(ZiplineIndex index) {
        this.index = index;
    }

    /**
     * Whether this block is part of a zipline that is keeping its path unbreakable.
     */
    public boolean isProtected(Block block) {
        return owner(block) != null;
    }

    /**
     * Returns the zipline protecting this block, or {@code null} if none is.
     */
    public Zipline owner(Block block) {
        for (Zipline zipline : index.nearby(block.getLocation(), SEARCH_RADIUS)) {
            ZiplineSettings settings = zipline.getSettings();
            if (!settings.isUnbreakable() || settings.getPathType() != PathType.BLOCK) {
                continue;
            }
            if (zipline.coversPathBlock(block.getX(), block.getY(), block.getZ())) {
                return zipline;
            }
        }
        return null;
    }
}
