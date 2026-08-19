package com.github.willrees23.zipline.path;

import com.github.willrees23.zipline.Zipline;
import org.bukkit.entity.Player;

/**
 * Draws a zipline's line in the world.
 *
 * <p>Implementations are stateless and shared between every zipline of their {@link PathType}.
 */
public interface PathRenderer {

    /**
     * Draws the line, returning how many non-air blocks were replaced so the caller can warn about
     * it. Renderers that do not touch the world return zero.
     */
    int apply(Zipline zipline);

    /** Undoes {@link #apply}, restoring anything it replaced. */
    void remove(Zipline zipline);

    /** Called regularly for each player near the zipline, for renderers that draw per viewer. */
    void tick(Zipline zipline, Player viewer);
}
