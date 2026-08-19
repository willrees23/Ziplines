package com.github.willrees23.util;

import lombok.experimental.UtilityClass;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Sends the plugin's chat messages in a consistent colour scheme: grey prose with the interesting
 * values picked out in yellow.
 */
@UtilityClass
public class ChatUtil {

    /** Colour applied to the surrounding prose when a message does not open with its own code. */
    private final String BASE = "&7";
    /** Colour applied to each substituted value. */
    private final String HIGHLIGHT = "&e";

    private final String PLACEHOLDER = "%s";
    private final char COLOR_CHAR = '&';
    private final int CODE_LENGTH = 2;

    public void sendColored(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes(COLOR_CHAR, message));
    }

    public void sendHighlighted(CommandSender sender, String template, Object... values) {
        sendColored(sender, highlight(template, values));
    }

    /**
     * Substitutes {@code values} into each {@code %s} in {@code template}, wrapping every
     * substitution in the highlight colour and restoring the surrounding colour afterwards.
     *
     * <p>Substitution is done by hand rather than with {@link String#format} so that a value
     * containing a percent sign is treated as text, and so that only the substituted spans are
     * recoloured. Recolouring by searching the finished message for each value would also catch
     * any occurrence of it in the surrounding prose.
     */
    public String highlight(String template, Object... values) {
        boolean coloured = template.length() >= CODE_LENGTH && template.charAt(0) == COLOR_CHAR;
        String base = coloured ? template.substring(0, CODE_LENGTH) : BASE;

        StringBuilder message = new StringBuilder();
        if (!coloured) {
            message.append(base);
        }

        int read = 0;
        for (Object value : values) {
            int placeholder = template.indexOf(PLACEHOLDER, read);
            if (placeholder < 0) {
                break;
            }
            message.append(template, read, placeholder)
                    .append(HIGHLIGHT)
                    .append(value)
                    .append(base);
            read = placeholder + PLACEHOLDER.length();
        }

        return message.append(template, read, template.length()).toString();
    }
}
