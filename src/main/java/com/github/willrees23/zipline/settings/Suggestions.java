package com.github.willrees23.zipline.settings;

import org.bukkit.Material;
import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Tab-completion candidates for {@link ZiplineOption}.
 *
 * <p>Every list is produced through a {@link Supplier} because the registry-backed ones cannot be
 * built until the server is up, and options are constructed while the enum class loads.
 */
final class Suggestions {

    private static final String SLAB_SUFFIX = "_SLAB";

    private static List<String> blocks;
    private static List<String> slabs;
    private static List<String> particles;
    private static List<String> sounds;

    private Suggestions() {
    }

    static Supplier<List<String>> of(String... values) {
        List<String> fixed = List.of(values);
        return () -> fixed;
    }

    static Supplier<List<String>> constants(Enum<?>[] values) {
        List<String> names = new ArrayList<>(values.length);
        for (Enum<?> value : values) {
            names.add(value.name());
        }
        List<String> fixed = List.copyOf(names);
        return () -> fixed;
    }

    // isLegacy is deprecated but remains the only way to spot the pre-flattening material
    // aliases, which are not worth offering as completions when the server exposes them.
    @SuppressWarnings("deprecation")
    static Supplier<List<String>> blocks() {
        return () -> {
            if (blocks == null) {
                List<String> names = new ArrayList<>();
                for (Material material : Material.values()) {
                    if (material.isBlock() && !material.isLegacy()) {
                        names.add(material.name());
                    }
                }
                blocks = List.copyOf(names);
            }
            return blocks;
        };
    }

    static Supplier<List<String>> slabs() {
        return () -> {
            if (slabs == null) {
                List<String> names = new ArrayList<>();
                for (String name : blocks().get()) {
                    if (name.endsWith(SLAB_SUFFIX)) {
                        names.add(name);
                    }
                }
                slabs = List.copyOf(names);
            }
            return slabs;
        };
    }

    static Supplier<List<String>> particles() {
        return () -> {
            if (particles == null) {
                List<String> names = new ArrayList<>();
                for (Particle particle : Particle.values()) {
                    if (particle.getDataType() == Void.class) {
                        names.add(particle.name());
                    }
                }
                particles = List.copyOf(names);
            }
            return particles;
        };
    }

    static Supplier<List<String>> sounds() {
        return () -> {
            if (sounds == null) {
                List<String> names = new ArrayList<>();
                names.add(ZiplineOption.NONE);
                names.addAll(Sounds.names());
                sounds = List.copyOf(names);
            }
            return sounds;
        };
    }
}
