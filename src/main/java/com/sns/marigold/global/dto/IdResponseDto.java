package com.sns.marigold.global.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "생성된 리소스 식별자")
public record IdResponseDto(@Schema(description = "생성된 리소스 ID", example = "123") String id) {}
