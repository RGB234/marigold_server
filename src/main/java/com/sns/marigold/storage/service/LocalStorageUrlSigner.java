package com.sns.marigold.storage.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sns.marigold.storage.config.LocalStorageProperties;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalStorageUrlSigner {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final LocalStorageProperties localStorageProperties;
  private final Clock clock;

  @Autowired
  public LocalStorageUrlSigner(LocalStorageProperties localStorageProperties) {
    this(localStorageProperties, Clock.systemUTC());
  }

  LocalStorageUrlSigner(LocalStorageProperties localStorageProperties, Clock clock) {
    this.localStorageProperties = localStorageProperties;
    this.clock = clock;
  }

  public long expiresAt(Duration ttl) {
    return Instant.now(clock).plus(ttl).getEpochSecond();
  }

  public String sign(String method, String path, long expiresAt, String filename) {
    return hmacSha256Hex(canonicalValue(method, path, expiresAt, filename));
  }

  public boolean isValid(
      String method, String path, Long expiresAt, String filename, String signature) {
    if (expiresAt == null
        || expiresAt < Instant.now(clock).getEpochSecond()
        || !StringUtils.hasText(signature)) {
      return false;
    }

    String expectedSignature = sign(method, path, expiresAt, filename);
    return MessageDigest.isEqual(
        expectedSignature.getBytes(StandardCharsets.UTF_8),
        signature.getBytes(StandardCharsets.UTF_8));
  }

  private String canonicalValue(String method, String path, long expiresAt, String filename) {
    return Objects.requireNonNull(method, "method must not be null")
        + "\n"
        + Objects.requireNonNull(path, "path must not be null")
        + "\n"
        + expiresAt
        + "\n"
        + Objects.requireNonNullElse(filename, "");
  }

  private String hmacSha256Hex(String value) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(signingSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("failed to sign local storage URL", e);
    }
  }

  private String signingSecret() {
    String secret = localStorageProperties.signingSecret();
    if (!StringUtils.hasText(secret)) {
      throw new IllegalStateException("local storage signing secret must not be blank");
    }
    return secret;
  }
}
