package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * Handles {@code /ziplines start <id> [speed]}, which marks the near end of a new zipline where the
 * player is stood.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplineStartCommand {

    @Dependency
    private ZiplineManager ziplines;

    /**
     * Taking a {@link Player} rather than a {@code CommandSender} is what limits this to players;
     * the console is turned away before the method is reached.
     *
     * <p>An absent {@code speed} arrives as {@code null}, which the manager reads as "use the
     * default from the config".
     */
    @Subcommand("start")
    @CommandPermission(ZiplinePermission.Node.START)
    public void start(Player player, String id, @Optional Double speed) {
        ziplines.startCreation(player, id, speed);
    }
}
