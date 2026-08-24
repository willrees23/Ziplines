package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * Handles {@code /ziplines cancel}, which throws away the zipline the player is part-way through
 * creating.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplineCancelCommand {

    @Dependency
    private ZiplineManager ziplines;

    /**
     * Guarded by the start permission rather than one of its own: whoever may begin a zipline may
     * abandon it.
     */
    @Subcommand("cancel")
    @CommandPermission(ZiplinePermission.Node.START)
    public void cancel(Player player) {
        ziplines.cancelCreation(player);
    }
}
