package com.github.willrees23.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatUtilTest {

    @Test
    @DisplayName("a substituted value is highlighted and the surrounding colour restored")
    void substitutionIsHighlighted() {
        assertEquals("&aCreated zipline &etest&a.", ChatUtil.highlight("&aCreated zipline %s.", "test"));
    }

    @Test
    @DisplayName("a message with no colour of its own is given the base colour")
    void baseColourIsAdded() {
        assertEquals("&7Usage: &e/zl list&7", ChatUtil.highlight("Usage: %s", "/zl list"));
    }

    @Test
    @DisplayName("prose that happens to contain the value is left alone")
    void proseIsNotRecoloured() {
        // The id "a" also appears in "and", "restored" and "path"; only the substitution is coloured.
        assertEquals("&aDeleted zipline &ea&a and restored its path.",
                ChatUtil.highlight("&aDeleted zipline %s and restored its path.", "a"));
    }

    @Test
    @DisplayName("a value containing a percent sign is treated as text")
    void percentSignsInValuesAreLiteral() {
        assertEquals("&7Deleted &e100%s&7", ChatUtil.highlight("Deleted %s", "100%s"));
    }

    @Test
    @DisplayName("values are substituted in order")
    void valuesAreSubstitutedInOrder() {
        assertEquals("&7Set &espeed&7 to &e2.0&7", ChatUtil.highlight("Set %s to %s", "speed", 2.0));
    }

    @Test
    @DisplayName("unfilled placeholders are left in place")
    void surplusPlaceholdersRemain() {
        assertEquals("&7Set &espeed&7 to %s", ChatUtil.highlight("Set %s to %s", "speed"));
    }

    @Test
    @DisplayName("surplus values are dropped rather than appended")
    void surplusValuesAreIgnored() {
        assertEquals("&7Set &espeed&7", ChatUtil.highlight("Set %s", "speed", "ignored"));
    }

    @Test
    @DisplayName("a message with no placeholders is returned as it stands")
    void messagesWithoutPlaceholders() {
        assertEquals("&cNo permission.", ChatUtil.highlight("&cNo permission."));
    }
}
