package com.github.willrees23.command;

import com.github.willrees23.util.ChatUtil;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;

/**
 * Handles a bare {@code /ziplines} by naming the sub-commands.
 *
 * <p>Each sub-command lives in its own class next to this one, and every one of them repeats the
 * same {@link Command} names so that Lamp hangs them all off the same command. The aliases are
 * declared here rather than in {@code plugin.yml}, which no longer mentions the command at all.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplinesCommand {

    /**
     * Shown whenever the sender has not picked a sub-command, or has picked one that does not
     * exist. Package-private so that {@link ZiplineExceptionHandler} can answer the latter with the
     * same line.
     */
    static final String USAGE = "/ziplines <start|end|cancel|delete|edit|list>";

    /**
     * Inherits the path of this class, so it answers {@code /ziplines}, {@code /zipline} and
     * {@code /zl} with no arguments.
     */
    @CommandPlaceholder
    public void usage(CommandSender sender) {
        ChatUtil.sendHighlighted(sender, "&7Usage: %s", USAGE);
    }
}
