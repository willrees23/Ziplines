package com.github.willrees23;

/**
 * The permission nodes declared in {@code plugin.yml}.
 */
public enum ZiplinePermission {

    ZIPLINE_START("ziplines.start"),
    ZIPLINE_END("ziplines.end"),
    ZIPLINE_DELETE("ziplines.delete"),
    ZIPLINE_EDIT("ziplines.edit"),
    ZIPLINE_USE("ziplines.use"),
    ZIPLINE_LIST("ziplines.list");

    private final String node;

    ZiplinePermission(String node) {
        this.node = node;
    }

    public String getNode() {
        return node;
    }
}
