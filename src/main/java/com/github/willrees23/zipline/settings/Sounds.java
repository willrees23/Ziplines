package com.github.willrees23.zipline.settings;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Looks sounds up by the screaming-snake-case names that {@code Sound} used to expose as enum
 * constants, for example {@code BLOCK_NOTE_BLOCK_BASS}.
 *
 * <p>Sound is now a registry rather than an enum, and {@link Registry#match(String)} only
 * understands namespaced keys such as {@code block.note_block.bass}. Configuration files written
 * before that change still hold the old names, so both spellings are accepted here and the old
 * spelling is what gets written back out.
 *
 * <p>The registry is only reachable once the server is running, so the lookup table is built on
 * first use rather than in a static initialiser.
 */
final class Sounds {

    private static final String MINECRAFT = NamespacedKey.MINECRAFT;

    private static Map<String, Sound> byName;
    private static List<String> names;

    private Sounds() {
    }

    static Sound parse(String value) {
        Sound sound = byName().get(value.toUpperCase(Locale.ROOT).replace('-', '_'));
        return sound != null ? sound : Registry.SOUNDS.match(value);
    }

    static String name(Sound sound) {
        NamespacedKey key = sound.getKeyOrThrow();
        if (!MINECRAFT.equals(key.getNamespace())) {
            return key.toString();
        }
        return key.getKey().toUpperCase(Locale.ROOT).replace('.', '_');
    }

    static List<String> names() {
        if (names == null) {
            names = List.copyOf(byName().keySet());
        }
        return names;
    }

    private static Map<String, Sound> byName() {
        if (byName == null) {
            Map<String, Sound> lookup = new HashMap<>();
            for (Sound sound : Registry.SOUNDS) {
                lookup.put(name(sound), sound);
            }
            byName = Map.copyOf(lookup);
        }
        return byName;
    }
}
