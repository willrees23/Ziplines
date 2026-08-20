package com.github.willrees23.zipline.settings;

/**
 * Which ends of a line a rider may board from.
 *
 * <p>A line runs from the point it was started at to the point it was finished at, so
 * {@code START_TO_END} is the way it was marked out and {@code END_TO_START} is the way back. The
 * end a one way line cannot be boarded from carries neither endpoint particles nor a seat, so
 * riders can see which way it runs.
 */
public enum RideDirection {

    BOTH,
    START_TO_END,
    END_TO_START;

    /**
     * Whether a ride may begin at the point the line was started from.
     */
    public boolean allowsStart() {
        return this != END_TO_START;
    }

    /**
     * Whether a ride may begin at the point the line was finished at.
     */
    public boolean allowsEnd() {
        return this != START_TO_END;
    }
}
