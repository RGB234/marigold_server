package com.sns.marigold.auth.common.recent;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.recent-auth")
public record RecentAuthProperties(long ttlSeconds) {}
