package com.github.willrees23.zipline;

import com.github.willrees23.zipline.path.PathProfile;
import com.github.willrees23.zipline.path.PlacedBlock;
import com.github.willrees23.zipline.settings.ZiplineSettings;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.List;

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

    /** Blocks the path replaced, in the order they were placed. Empty for path types that place none. */
    @Setter
    private List<PlacedBlock> placedBlocks = List.of();

    private PathProfile profile;

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
    }

    /** Returns the ride profile, building it on first use and again whenever the end point moves. */
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
