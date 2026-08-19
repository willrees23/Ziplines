package com.github.willrees23.zipline.path;

import com.github.willrees23.zipline.Zipline;
import org.bukkit.entity.Player;

public interface PathRenderer {

    int apply(Zipline zipline);

    void remove(Zipline zipline);

    void tick(Zipline zipline, Player viewer);
}
