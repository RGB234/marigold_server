package com.sns.marigold.auth.common.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;

/**
 * refresh token 값은 {@code {sid}.{비밀값}}입니다. 서명이나 claim이 없고, 유효한지는 Redis 세션에 저장한 비밀값의 SHA-256과 비교해서만
 * 정합니다. 해시만 저장하므로 Redis 내용이 새어도 token으로 쓸 수 없습니다.
 */
public record RefreshToken(String sessionId, String secret) {

  private static final int SECRET_BYTES = 32;
  private static final int MAX_SESSION_ID_LENGTH = 64;
  // 32바이트를 padding 없는 base64url로 쓰면 43자다. JWT처럼 점이 더 있는 값은 여기서 걸러진다
  private static final Pattern SECRET_PATTERN = Pattern.compile("[A-Za-z0-9_-]{43}");
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

  public static RefreshToken issue(String sessionId) {
    byte[] secret = new byte[SECRET_BYTES];
    RANDOM.nextBytes(secret);
    return new RefreshToken(sessionId, ENCODER.encodeToString(secret));
  }

  /** 형식이 맞지 않으면 null을 돌려줍니다. 세션이 있는지는 보지 않습니다. */
  @Nullable
  public static RefreshToken parse(@Nullable String value) {
    if (value == null) {
      return null;
    }
    int separator = value.indexOf('.');
    if (separator <= 0 || separator > MAX_SESSION_ID_LENGTH) {
      return null;
    }
    String secret = value.substring(separator + 1);
    if (!SECRET_PATTERN.matcher(secret).matches()) {
      return null;
    }
    return new RefreshToken(value.substring(0, separator), secret);
  }

  /** 쿠키에 담는 값입니다. */
  public String value() {
    return sessionId + "." + secret;
  }

  public String secretHash() {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.US_ASCII));
      return ENCODER.encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  // 실수로 로그에 남아도 비밀값이 드러나지 않게 한다
  @Override
  public String toString() {
    return "RefreshToken[sessionId=" + sessionId + "]";
  }
}
