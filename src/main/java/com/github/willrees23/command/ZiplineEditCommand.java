package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.annotation.CommandPermission;

/**
 * Handles {@code /ziplines edit <id> <option> <value>}, which changes one setting on an existing
 * zipline.
 *
 * <p>The option and the value are passed on as text: which options exist, and what each one accepts,
 * is the manager's business, and it reports anything it does not recognise itself.
 */
@Command({"ziplines", "zipline", "zl"})
public class ZiplineEditCommand {

    @Dependency
    private ZiplineManager ziplines;

    @Subcommand("edit")
    @CommandPermission(ZiplinePermission.Node.EDIT)
    public void edit(CommandSender sender,
                     @ZiplineId String id,
                     @SuggestWith(ZiplineSuggestions.Options.class) String option,
                     @SuggestWith(ZiplineSuggestions.Values.class) String value) {
        ziplines.edit(sender, id, option, value);
    }
}
