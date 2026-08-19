package com.github.willrees23.command;

import com.github.willrees23.ZiplinePermission;
import com.github.willrees23.util.ChatUtil;
import com.github.willrees23.zipline.ZiplineManager;
import com.github.willrees23.zipline.settings.ZiplineOption;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles {@code /ziplines} and its aliases.
 */
public class ZiplinesCommand implements TabExecutor {

    private static final String USAGE = "/ziplines <start|end|cancel|delete|edit|list>";
    private static final List<String> SUB_COMMANDS = List.of("start", "end", "cancel", "delete", "edit", "list");

    private final ZiplineManager ziplines;

    public ZiplinesCommand(ZiplineManager ziplines) {
        this.ziplines = ziplines;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ChatUtil.sendHighlighted(sender, "&7Usage: %s", USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        return switch (subCommand) {
            case "list" -> list(sender);
            case "delete" -> delete(sender, args);
            case "edit" -> edit(sender, args);
            default -> runAsPlayer(sender, subCommand, args);
        };
    }

    private boolean list(CommandSender sender) {
        if (allowed(sender, ZiplinePermission.ZIPLINE_LIST)) {
            ziplines.list(sender);
        }
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (!allowed(sender, ZiplinePermission.ZIPLINE_DELETE)) {
            return true;
        }
        if (args.length < 2) {
            ChatUtil.sendHighlighted(sender, "&7Usage: %s", "/zl delete <id>");
            return true;
        }
        ziplines.delete(sender, args[1]);
        return true;
    }

    private boolean edit(CommandSender sender, String[] args) {
        if (!allowed(sender, ZiplinePermission.ZIPLINE_EDIT)) {
            return true;
        }
        if (args.length < 4) {
            ChatUtil.sendHighlighted(sender, "&7Usage: %s. Options: %s",
                    "/zl edit <id> <option> <value>", String.join(", ", ZiplineOption.keys()));
            return true;
        }
        ziplines.edit(sender, args[1], args[2], args[3]);
        return true;
    }

    /**
     * The remaining sub-commands act on where the sender is stood, so they need a player.
     */
    private boolean runAsPlayer(CommandSender sender, String subCommand, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtil.sendColored(sender, "&cOnly players can use this command.");
            return true;
        }

        switch (subCommand) {
            case "start" -> start(player, args);
            case "end" -> {
                if (allowed(player, ZiplinePermission.ZIPLINE_END)) {
                    ziplines.endCreation(player);
                }
            }
            case "cancel" -> {
                if (allowed(player, ZiplinePermission.ZIPLINE_START)) {
                    ziplines.cancelCreation(player);
                }
            }
            default -> ChatUtil.sendHighlighted(player, "&7Usage: %s", USAGE);
        }
        return true;
    }

    private void start(Player player, String[] args) {
        if (!allowed(player, ZiplinePermission.ZIPLINE_START)) {
            return;
        }
        if (args.length < 2) {
            ChatUtil.sendHighlighted(player, "&7Usage: %s", "/zl start <id> [speed]");
            return;
        }

        Double speed = null;
        if (args.length >= 3) {
            speed = parseSpeed(args[2]);
            if (speed == null) {
                ChatUtil.sendHighlighted(player, "&cInvalid speed %s.", args[2]);
                return;
            }
        }
        ziplines.startCreation(player, args[1], speed);
    }

    private boolean allowed(CommandSender sender, ZiplinePermission permission) {
        if (sender.hasPermission(permission.getNode())) {
            return true;
        }
        ChatUtil.sendColored(sender, "&cYou do not have permission to do that.");
        return false;
    }

    private Double parseSpeed(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return matching(SUB_COMMANDS, args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        boolean editing = subCommand.equals("edit");
        if (args.length == 2 && (editing || subCommand.equals("delete"))) {
            return matching(ziplines.getIds(), args[1]);
        }
        if (args.length == 3 && editing) {
            return matching(ZiplineOption.keys(), args[2]);
        }
        if (args.length == 4 && editing) {
            ZiplineOption option = ZiplineOption.fromKey(args[2]);
            return option == null ? List.of() : matching(option.getSuggestions(), args[3]);
        }
        return List.of();
    }

    private List<String> matching(List<String> values, String prefix) {
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
                matches.add(value);
            }
        }
        return matches;
    }
}
