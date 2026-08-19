package com.github.willrees23.zipline.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZiplineOptionTest {

    @Test
    @DisplayName("every option has a distinct key")
    void keysAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ZiplineOption option : ZiplineOption.values()) {
            assertTrue(seen.add(option.getKey()), "duplicate key " + option.getKey());
        }
        assertEquals(ZiplineOption.values().length, ZiplineOption.keys().size());
    }

    @Test
    @DisplayName("keys are lower case and hyphenated, so they read well in configuration")
    void keysAreConfigurationFriendly() {
        for (ZiplineOption option : ZiplineOption.values()) {
            assertTrue(option.getKey().matches("[a-z][a-z-]*[a-z]"), "awkward key " + option.getKey());
        }
    }

    @Test
    @DisplayName("keys never contain a dot, which configuration would read as nesting")
    void keysAreNotConfigurationPaths() {
        for (ZiplineOption option : ZiplineOption.values()) {
            assertTrue(option.getKey().indexOf('.') < 0, "nested key " + option.getKey());
        }
    }

    @Test
    @DisplayName("options are found by key regardless of case")
    void lookupIsCaseInsensitive() {
        assertSame(ZiplineOption.RIDE_SOUND_PITCH_START, ZiplineOption.fromKey("ride-sound-pitch-start"));
        assertSame(ZiplineOption.RIDE_SOUND_PITCH_START, ZiplineOption.fromKey("Ride-Sound-Pitch-Start"));
    }

    @Test
    @DisplayName("an unknown key yields nothing rather than failing")
    void unknownKeysReturnNull() {
        assertNull(ZiplineOption.fromKey("nonsense"));
    }
}
