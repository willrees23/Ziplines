package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * Handles {@code /ziplines end}, which marks the far end of the zipline the player is creating and
 * builds it.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplineEndCommand {

    @Dependency
    private ZiplineManager ziplines;

    @Subcommand("end")
    @CommandPermission(ZiplinePermission.Node.END)
    public void end(Player player) {
        ziplines.endCreation(player);
    }
}
