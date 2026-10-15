package com.sns.marigold.auth.common.csrf;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.csrf")
public record CsrfProperties(String secretKey) {

  private static final int MINIMUM_SECRET_BYTES = 32;

  public CsrfProperties {
    if (secretKey == null
        || secretKey.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
      throw new IllegalArgumentException("CSRF secret key must be at least 32 bytes");
    }
  }
}
