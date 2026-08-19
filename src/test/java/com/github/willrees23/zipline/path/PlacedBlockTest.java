package com.github.willrees23.zipline.path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlacedBlockTest {

    @Test
    @DisplayName("a stored block reads back exactly as it was written")
    void roundTrips() {
        PlacedBlock block = new PlacedBlock(12, -60, 3400, "minecraft:oak_fence[east=true,north=false]");

        assertEquals(block, PlacedBlock.parse(block.serialize()));
    }

    @Test
    @DisplayName("block data keeps any separators of its own")
    void blockDataMayContainSeparators() {
        PlacedBlock block = new PlacedBlock(0, 0, 0, "minecraft:sign;weird");

        assertEquals("minecraft:sign;weird", PlacedBlock.parse(block.serialize()).data());
    }

    @Test
    @DisplayName("entries that are too short or not numeric are rejected")
    void malformedEntriesAreRejected() {
        assertNull(PlacedBlock.parse(""));
        assertNull(PlacedBlock.parse("1;2;3"));
        assertNull(PlacedBlock.parse("1;2;three;minecraft:stone"));
    }
}
