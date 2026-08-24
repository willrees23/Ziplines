package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * Handles {@code /ziplines list}, which names the ziplines on the server.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplineListCommand {

    @Dependency
    private ZiplineManager ziplines;

    @Subcommand("list")
    @CommandPermission(ZiplinePermission.Node.LIST)
    public void list(CommandSender sender) {
        ziplines.list(sender);
    }
}
