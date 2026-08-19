package com.github.willrees23.zipline.path;

/**
 * How a zipline's line is drawn in the world.
 *
 * <p>The renderers are stateless, so a single instance per type is shared by every zipline.
 */
public enum PathType {

    /** Places real blocks along the line, remembering what they replaced. */
    BLOCK(new BlockPathRenderer()),
    /** Draws the line with particles, leaving the world untouched. */
    PARTICLE(new ParticlePathRenderer());

    private final PathRenderer renderer;

    PathType(PathRenderer renderer) {
        this.renderer = renderer;
    }

    public PathRenderer getRenderer() {
        return renderer;
    }
}
