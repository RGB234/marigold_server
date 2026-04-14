package com.sns.marigold.chat.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ChatRoomType {
    WRITER,
    INQUIRER,
    ALL;

    @JsonCreator
    public static ChatRoomType fromString(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return ChatRoomType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
