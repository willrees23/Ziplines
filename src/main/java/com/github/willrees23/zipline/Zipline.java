package com.github.willrees23.zipline;

import com.github.willrees23.zipline.path.PathProfile;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Zipline {

    private final String id;
    private final Location start;
    private Location end;
    private final ZiplineSettings settings;
    @Setter
    private List<String> placedBlocks = new ArrayList<>();

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
