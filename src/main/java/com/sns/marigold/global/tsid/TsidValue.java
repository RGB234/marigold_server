package com.sns.marigold.global.tsid;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(type = "string", description = "Crockford Base32 형식 TSID", example = "01JABCDEF1234")
public record TsidValue(long value) {}
