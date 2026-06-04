package com.sns.marigold.auth.common.csrf;

import com.sns.marigold.auth.common.util.CookieManager;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CsrfTokenService {

  public static final String CSRF_TOKEN_COOKIE_NAME = "XSRF-TOKEN";
  public static final String CSRF_TOKEN_HEADER_NAME = "X-CSRF-TOKEN";
  private static final int TOKEN_BYTES = 32;

  private final CookieManager cookieManager;
  private final SecureRandom secureRandom = new SecureRandom();

  @Value("${jwt.refresh-token-validity-in-seconds:86400}")
  private long tokenValidityInSeconds;

  public void issue(HttpServletResponse response) {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    cookieManager.addReadableCookie(
        response, CSRF_TOKEN_COOKIE_NAME, token, tokenValidityInSeconds);
    response.setHeader(CSRF_TOKEN_HEADER_NAME, token);
  }

  public void clear(HttpServletResponse response) {
    cookieManager.expireCookie(response, CSRF_TOKEN_COOKIE_NAME);
  }
}
