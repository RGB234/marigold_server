package com.sns.marigold.auth.common.recent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.auth.recent-auth")
public record RecentAuthProperties(long ttlSeconds) {}
