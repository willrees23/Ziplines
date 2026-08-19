package com.github.willrees23.enums;

import com.github.willrees23.zipline.path.BlockPathRenderer;
import com.github.willrees23.zipline.path.ParticlePathRenderer;
import com.github.willrees23.zipline.path.PathRenderer;
import lombok.Getter;

@Getter
public enum PathType {

    BLOCK(new BlockPathRenderer()),
    PARTICLE(new ParticlePathRenderer());

    private final PathRenderer renderer;

    PathType(PathRenderer renderer) {
        this.renderer = renderer;
    }
}
