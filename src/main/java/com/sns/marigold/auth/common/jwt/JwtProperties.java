package com.sns.marigold.auth.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.jwt")
public record JwtProperties(Token access) {

  public record Token(String secret, long ttlSeconds) {}
}
