package com.sns.marigold.global.error.dto;

import lombok.Getter;

@Getter
public class FieldErrorDetail implements ErrorDetail {
    private final String field;
    private final String message;

    public FieldErrorDetail(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
