package com.github.willrees23.task;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * A scheduler task that is started and stopped as work arrives, so the plugin does not keep a timer
 * ticking while there is nothing to do.
 *
 * <p>Both methods are idempotent, which lets callers stop a task whenever their last piece of work
 * finishes without first checking whether it is running.
 */
public final class RepeatingTask {

    private final Plugin plugin;
    private final long interval;
    private final Runnable action;

    private BukkitTask task;

    public RepeatingTask(Plugin plugin, long interval, Runnable action) {
        this.plugin = plugin;
        this.interval = interval;
        this.action = action;
    }

    public void start() {
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, action, 0L, interval);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
