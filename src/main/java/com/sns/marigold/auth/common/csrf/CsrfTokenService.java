package com.sns.marigold.auth.common.csrf;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.common.util.CookieManager;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsrfTokenService {

  public static final String CSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";
  public static final String CSRF_TOKEN_HEADER_NAME = "X-CSRF-TOKEN";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final int RANDOM_VALUE_BYTES = 32;
  private static final int BASE64_URL_SHA256_LENGTH = 43;

  private final CookieManager cookieManager;
  private final JwtProperties jwtProperties;
  private final CsrfProperties csrfProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  public void issue(HttpServletResponse response, String sessionBinding) {
    String token = createToken(sessionBinding);
    cookieManager.addReadableCookie(
        response, CSRF_TOKEN_COOKIE_NAME, token, jwtProperties.refreshTokenValidityInSeconds());
    response.setHeader(CSRF_TOKEN_HEADER_NAME, token);
  }

  public void issue(HttpServletRequest request, HttpServletResponse response) {
    String sessionBinding = resolveSessionBinding(request);
    if (sessionBinding != null) {
      issue(response, sessionBinding);
    }
  }

  public boolean isValid(
      HttpServletRequest request, @Nullable String cookieToken, @Nullable String headerToken) {
    return isValid(cookieToken, headerToken, resolveSessionBinding(request));
  }

  public boolean isValid(
      @Nullable String cookieToken, @Nullable String headerToken, @Nullable String sessionBinding) {
    if (!constantTimeEquals(cookieToken, headerToken) || sessionBinding == null) {
      return false;
    }

    String[] tokenParts = headerToken.split("\\.", -1);
    if (tokenParts.length != 2) {
      return false;
    }
    if (tokenParts[0].length() != BASE64_URL_SHA256_LENGTH
        || tokenParts[1].length() != BASE64_URL_SHA256_LENGTH) {
      return false;
    }

    byte[] actualHmac;
    byte[] randomValue;
    try {
      actualHmac = Base64.getUrlDecoder().decode(tokenParts[0]);
      randomValue = Base64.getUrlDecoder().decode(tokenParts[1]);
    } catch (IllegalArgumentException e) {
      return false;
    }

    if (actualHmac.length != 32 || randomValue.length != RANDOM_VALUE_BYTES) {
      return false;
    }

    byte[] expectedHmac = sign(sessionBinding, tokenParts[1]);
    return MessageDigest.isEqual(actualHmac, expectedHmac);
  }

  @Nullable
  public String resolveSessionBinding(HttpServletRequest request) {
    Cookie refreshCookie = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);
    if (hasValue(refreshCookie)) {
      return refreshCookie.getValue();
    }

    Cookie recentAuthCookie =
        cookieManager.getCookie(request, CookieManager.RECENT_AUTH_TOKEN_NAME);
    return hasValue(recentAuthCookie) ? recentAuthCookie.getValue() : null;
  }

  public void clear(HttpServletResponse response) {
    cookieManager.expireCookie(response, CSRF_TOKEN_COOKIE_NAME);
  }

  private String createToken(String sessionBinding) {
    if (sessionBinding == null || sessionBinding.isBlank()) {
      throw new IllegalArgumentException("CSRF session binding must not be blank");
    }

    byte[] randomBytes = new byte[RANDOM_VALUE_BYTES];
    secureRandom.nextBytes(randomBytes);
    String randomValue = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    String hmac =
        Base64.getUrlEncoder().withoutPadding().encodeToString(sign(sessionBinding, randomValue));
    return hmac + "." + randomValue;
  }

  private byte[] sign(String sessionBinding, String randomValue) {
    String message =
        sessionBinding.length()
            + "!"
            + sessionBinding
            + "!"
            + randomValue.length()
            + "!"
            + randomValue;
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(
              csrfProperties.secretKey().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HMAC-SHA256 is unavailable", e);
    }
  }

  private boolean constantTimeEquals(@Nullable String left, @Nullable String right) {
    if (left == null || right == null) {
      return false;
    }
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }

  private boolean hasValue(@Nullable Cookie cookie) {
    return cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank();
  }
}
