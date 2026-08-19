package com.github.willrees23.command;

import com.github.willrees23.enums.ZiplineOption;
import com.github.willrees23.enums.ZiplinePermission;
import com.github.willrees23.util.ChatUtil;
import com.github.willrees23.zipline.Zipline;
import com.github.willrees23.zipline.ZiplineManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ZiplinesCommand implements TabExecutor {

    private static final String USAGE = "/ziplines <start|end|cancel|delete|edit|list>";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            ChatUtil.sendHighlighted(sender, "&7Usage: %s", USAGE);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "list":
                if (!hasPermission(sender, ZiplinePermission.ZIPLINE_LIST)) {
                    return true;
                }
                ZiplineManager.getInstance().list(sender);
                return true;
            case "delete":
                if (!hasPermission(sender, ZiplinePermission.ZIPLINE_DELETE)) {
                    return true;
                }
                if (args.length < 2) {
                    ChatUtil.sendHighlighted(sender, "&7Usage: %s", "/zl delete <id>");
                    return true;
                }
                ZiplineManager.getInstance().delete(sender, args[1]);
                return true;
            case "edit":
                if (!hasPermission(sender, ZiplinePermission.ZIPLINE_EDIT)) {
                    return true;
                }
                if (args.length < 4) {
                    ChatUtil.sendHighlighted(sender, "&7Usage: %s. Options: %s", "/zl edit <id> <option> <value>", String.join(", ", ZiplineOption.keys()));
                    return true;
                }
                ZiplineManager.getInstance().edit(sender, args[1], args[2], args[3]);
                return true;
            default:
                break;
        }

        if (!(sender instanceof Player player)) {
            ChatUtil.sendColored(sender, "&cOnly players can use this command.");
            return true;
        }

        switch (subCommand) {
            case "start":
                if (!hasPermission(player, ZiplinePermission.ZIPLINE_START)) {
                    return true;
                }
                if (args.length < 2) {
                    ChatUtil.sendHighlighted(player, "&7Usage: %s", "/zl start <id> [speed]");
                    return true;
                }
                ZiplineManager.getInstance().startCreation(player, args[1], parseSpeed(args));
                break;
            case "end":
                if (!hasPermission(player, ZiplinePermission.ZIPLINE_END)) {
                    return true;
                }
                ZiplineManager.getInstance().endCreation(player);
                break;
            case "cancel":
                if (!hasPermission(player, ZiplinePermission.ZIPLINE_START)) {
                    return true;
                }
                ZiplineManager.getInstance().cancelCreation(player);
                break;
            default:
                ChatUtil.sendHighlighted(player, "&7Usage: %s", USAGE);
                break;
        }
        return true;
    }

    private boolean hasPermission(CommandSender sender, ZiplinePermission permission) {
        if (sender.hasPermission(permission.getPermission())) {
            return true;
        }
        ChatUtil.sendColored(sender, "&cYou do not have permission to do that.");
        return false;
    }

    private Double parseSpeed(String[] args) {
        if (args.length < 3) {
            return null;
        }
        try {
            return Double.parseDouble(args[2]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filter(List.of("start", "end", "cancel", "delete", "edit", "list"), args[0]);
        }

        String subCommand = args[0].toLowerCase();
        if (args.length == 2 && (subCommand.equals("delete") || subCommand.equals("edit"))) {
            return filter(ids(), args[1]);
        }
        if (args.length == 3 && subCommand.equals("edit")) {
            return filter(ZiplineOption.keys(), args[2]);
        }
        if (args.length == 4 && subCommand.equals("edit")) {
            ZiplineOption option = ZiplineOption.fromKey(args[2]);
            return option == null ? List.of() : filter(option.getSuggestions(), args[3]);
        }
        return List.of();
    }

    private List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (Zipline zipline : ZiplineManager.getInstance().getZiplines()) {
            ids.add(zipline.getId());
        }
        return ids;
    }

    private List<String> filter(List<String> values, String prefix) {
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(prefix.toLowerCase())) {
                matches.add(value);
            }
        }
        return matches;
    }
}
