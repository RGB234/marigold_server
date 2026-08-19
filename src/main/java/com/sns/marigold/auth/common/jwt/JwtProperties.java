package com.sns.marigold.auth.common.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secretKey, long accessTokenValidityInSeconds, long refreshTokenValidityInSeconds) {}
