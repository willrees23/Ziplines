package com.github.willrees23.command;

import com.github.willrees23.util.ChatUtil;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.exception.BukkitExceptionHandler;
import revxrsal.commands.bukkit.exception.SenderNotPlayerException;
import revxrsal.commands.exception.ExpectedLiteralException;
import revxrsal.commands.exception.RuntimeExceptionAdapter.HandleException;
import revxrsal.commands.exception.MissingArgumentException;
import revxrsal.commands.exception.NoPermissionException;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ParameterNode;

/**
 * Rewords the handful of errors Lamp raises for this plugin's commands, so that a mistyped command
 * still reads in the plugin's own voice.
 *
 * <p>Lamp finds these by their annotation rather than by their being overrides, so each one carries
 * {@link HandleException} of its own; an override does not inherit it.
 */
public class ZiplineExceptionHandler extends BukkitExceptionHandler {

    @Override
    @HandleException
    public void onNoPermission(NoPermissionException exception, BukkitCommandActor actor) {
        ChatUtil.sendColored(actor.sender(), "&cYou do not have permission to do that.");
    }

    @Override
    @HandleException
    public void onSenderNotPlayer(SenderNotPlayerException exception, BukkitCommandActor actor) {
        ChatUtil.sendColored(actor.sender(), "&cOnly players can use this command.");
    }

    /**
     * Lamp knows the usage of the command being run, so a half-typed sub-command is answered with
     * its own line rather than with the list of every sub-command.
     */
    @Override
    @HandleException
    public void onMissingArgument(MissingArgumentException exception,
                                  BukkitCommandActor actor,
                                  ParameterNode<BukkitCommandActor, ?> parameter) {
        ChatUtil.sendHighlighted(actor.sender(), "&7Usage: %s", "/" + parameter.command().usage());
    }

    /**
     * Raised when the word after {@code /ziplines} is not a sub-command at all, which is worth
     * answering with the full list.
     */
    @Override
    @HandleException
    public void onExpectedLiteral(ExpectedLiteralException exception, BukkitCommandActor actor) {
        ChatUtil.sendHighlighted(actor.sender(), "&7Usage: %s", ZiplinesCommand.USAGE);
    }

    @Override
    @HandleException
    public void onUnknownCommand(UnknownCommandException exception, BukkitCommandActor actor) {
        ChatUtil.sendHighlighted(actor.sender(), "&7Usage: %s", ZiplinesCommand.USAGE);
    }
}
