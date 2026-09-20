package com.sns.marigold.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.local")
public record LocalStorageProperties(
    String rootPath,
    String publicBaseUrl,
    String signingSecret,
    long viewUrlTtlMinutes,
    long downloadUrlTtlMinutes) {}
