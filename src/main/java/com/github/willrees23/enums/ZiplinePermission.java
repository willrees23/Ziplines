package com.github.willrees23.enums;

import lombok.Getter;

@Getter
public enum ZiplinePermission {

    ZIPLINE_START("ziplines.start"),
    ZIPLINE_END("ziplines.end"),
    ZIPLINE_DELETE("ziplines.delete"),
    ZIPLINE_EDIT("ziplines.edit"),
    ZIPLINE_USE("ziplines.use"),
    ZIPLINE_LIST("ziplines.list");

    private final String permission;

    ZiplinePermission(String permission) {
        this.permission = permission;
    }
}
