package com.github.willrees23.zipline;

import com.github.willrees23.zipline.path.PathGeometry;
import com.github.willrees23.zipline.path.PathProfile;
import com.github.willrees23.zipline.path.PlacedBlock;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;

/**
 * One zipline: two endpoints in a world, the settings that govern how it looks and rides, and a
 * record of the blocks its path replaced.
 *
 * <p>A zipline exists in an unfinished state between {@code /zl start} and {@code /zl end}, during
 * which {@link #getEnd()} is {@code null}.
 */
@Getter
public class Zipline {

    private final String id;
    private final Location start;
    private final ZiplineSettings settings;

    private Location end;

    /**
     * Blocks the path replaced, in the order they were placed. Empty for path types that place none.
     */
    @Setter
    private List<PlacedBlock> placedBlocks = List.of();

    private PathProfile profile;

    /**
     * The blocks of the path, packed for lookup. Built on demand and dropped when the end moves.
     */
    @Getter(AccessLevel.NONE)
    private Set<Long> pathBlockKeys;

    public Zipline(String id, Location start, ZiplineSettings settings) {
        this(id, start, null, settings);
    }

    public Zipline(String id, Location start, Location end, ZiplineSettings settings) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.settings = settings;
    }

    public void setEnd(Location end) {
        this.end = end;
        this.profile = null;
        this.pathBlockKeys = null;
    }

    /**
     * Whether the line runs through the given block.
     *
     * <p>Answered from the line's geometry rather than from {@link #getPlacedBlocks()}, which the
     * renderer only fills with the blocks it actually replaced. The two differ wherever the path
     * ran through a block that was already the right material: the renderer leaves such a block
     * alone and so never records it, but it is part of the line all the same.
     */
    public boolean coversPathBlock(int x, int y, int z) {
        if (end == null) {
            return false;
        }
        if (pathBlockKeys == null) {
            pathBlockKeys = PathGeometry.pathBlockKeys(start, end);
        }
        return pathBlockKeys.contains(PathGeometry.blockKey(x, y, z));
    }

    /**
     * Returns the ride profile, building it on first use and again whenever the end point moves.
     */
    public PathProfile getProfile() {
        if (profile == null) {
            profile = new PathProfile(start, end);
        }
        return profile;
    }

    public double getLength() {
        return start.distance(end);
    }
}
