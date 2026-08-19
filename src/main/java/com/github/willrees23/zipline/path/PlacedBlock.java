package com.github.willrees23.zipline.path;

/**
 * A block that the path renderer overwrote, together with the block data that was there before, so
 * that deleting a zipline can put the world back the way it was.
 *
 * <p>The text form is what gets written to {@code ziplines.yml}.
 *
 * @param x    block x coordinate
 * @param y    block y coordinate
 * @param z    block z coordinate
 * @param data the replaced block data, in the form {@code org.bukkit.block.data.BlockData} parses
 */
public record PlacedBlock(int x, int y, int z, String data) {

    private static final String SEPARATOR = ";";
    private static final int FIELDS = 4;

    /**
     * Parses a stored record, returning {@code null} if it is malformed.
     */
    public static PlacedBlock parse(String value) {
        String[] parts = value.split(SEPARATOR, FIELDS);
        if (parts.length < FIELDS) {
            return null;
        }
        try {
            return new PlacedBlock(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), parts[3]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public String serialize() {
        return x + SEPARATOR + y + SEPARATOR + z + SEPARATOR + data;
    }
}
