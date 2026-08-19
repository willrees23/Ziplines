package com.github.willrees23.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@UtilityClass
public class ChatUtil {

    public final String GENERAL = "&7";
    public final String HIGHLIGHT = "&e";

    public void send(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    public void sendColored(CommandSender sender, String message) {
        send(sender, ChatColor.translateAlternateColorCodes('&', message));
    }

    public void sendHighlighted(CommandSender sender, String message, String... highlights) {
        String formattedMessage = String.format(message, (Object[]) highlights);
        if (!formattedMessage.startsWith("&")) {
            formattedMessage = GENERAL + formattedMessage;
        }

        String baseColor = formattedMessage.substring(0, 2);
        for (String highlight : highlights) {
            formattedMessage = formattedMessage.replace(highlight, HIGHLIGHT + highlight + baseColor);
        }
        sendColored(sender, formattedMessage);
    }
}
