package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * Handles {@code /ziplines delete <id>}, which removes a zipline and restores the blocks its path
 * replaced.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplineDeleteCommand {

    @Dependency
    private ZiplineManager ziplines;

    @Subcommand("delete")
    @CommandPermission(ZiplinePermission.Node.DELETE)
    public void delete(CommandSender sender, @ZiplineId String id) {
        ziplines.delete(sender, id);
    }
}
