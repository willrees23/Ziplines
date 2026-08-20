package com.github.willrees23.zipline.seat;

import com.github.willrees23.task.RepeatingTask;
import com.github.willrees23.zipline.Zipline;
import org.bukkit.Location;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Drives the seats that are making their own way back to the end they belong to.
 *
 * <p>Only a line that carries one rider at a time lends its seat out, so a zipline can have at most
 * one seat on its way home, and a line with one is not boardable until it arrives.
 *
 * <p>The task runs only while there is a seat to move, and every tick while there is, so that a
 * returning seat travels as smoothly as a ride does.
 */
final class SeatReturnTask {

    private static final long INTERVAL = 1L;

    private final Map<String, SeatReturn> returning = new HashMap<>();
    private final RepeatingTask task;

    SeatReturnTask(Plugin plugin) {
        this.task = new RepeatingTask(plugin, INTERVAL, this::tick);
    }

    /**
     * Sends a seat home from wherever the ride left it.
     *
     * @param from the end of the line it is coming back from
     * @param to   the end it belongs to
     * @param home where it parks at that end
     */
    void start(Zipline zipline, BlockDisplay display, Location from, Location to, Location home) {
        returning.put(zipline.getId(), new SeatReturn(zipline, display, from, to, home));
        task.start();
    }

    boolean isReturning(Zipline zipline) {
        return returning.containsKey(zipline.getId());
    }

    /**
     * Stops tracking a zipline's returning seat without touching the display, for when the seats of
     * that line are being taken away or replaced anyway.
     */
    void forget(Zipline zipline) {
        if (returning.remove(zipline.getId()) != null) {
            stopWhenIdle();
        }
    }

    void forgetAll() {
        returning.clear();
        task.stop();
    }

    private void tick() {
        returning.values().removeIf(seat -> !seat.tick());
        stopWhenIdle();
    }

    private void stopWhenIdle() {
        if (returning.isEmpty()) {
            task.stop();
        }
    }
}
