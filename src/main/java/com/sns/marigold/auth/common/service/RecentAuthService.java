package com.sns.marigold.auth.common.service;

import com.sns.marigold.auth.common.recent.RecentAuthStore;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecentAuthService {

  private final RecentAuthStore recentAuthStore;
  private final CookieManager cookieManager;

  @Value("${auth.recent-auth.ttl-seconds:300}")
  private long ttlSeconds;

  public void issue(HttpServletResponse response, Long userId) {
    String token = UUID.randomUUID().toString();
    recentAuthStore.save(token, userId, Instant.now().plusSeconds(ttlSeconds));
    cookieManager.addCookie(response, CookieManager.RECENT_AUTH_TOKEN_NAME, token, ttlSeconds);
  }

  public void validate(HttpServletRequest request, Long expectedUserId) {
    String token = getRecentAuthToken(request);
    boolean valid = recentAuthStore.isValid(token, expectedUserId, Instant.now());

    if (!valid) {
      throw AuthException.forRecentAuthRequired();
    }
  }

  public void clear(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = cookieManager.getCookie(request, CookieManager.RECENT_AUTH_TOKEN_NAME);
    if (cookie != null) {
      recentAuthStore.remove(cookie.getValue());
    }

    cookieManager.expireCookie(response, CookieManager.RECENT_AUTH_TOKEN_NAME);
  }

  private String getRecentAuthToken(HttpServletRequest request) {
    Cookie cookie = cookieManager.getCookie(request, CookieManager.RECENT_AUTH_TOKEN_NAME);
    if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
      throw AuthException.forRecentAuthRequired();
    }

    return cookie.getValue();
  }
}
