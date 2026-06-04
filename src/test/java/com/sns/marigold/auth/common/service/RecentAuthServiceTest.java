package com.sns.marigold.auth.common.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sns.marigold.auth.common.recent.RecentAuthStore;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class RecentAuthServiceTest {

  @Mock private RecentAuthStore recentAuthStore;
  @Mock private CookieManager cookieManager;

  private RecentAuthService recentAuthService;

  @BeforeEach
  void setUp() {
    recentAuthService = new RecentAuthService(recentAuthStore, cookieManager);
    ReflectionTestUtils.setField(recentAuthService, "ttlSeconds", 300L);
  }

  @Test
  void issue_SavesTokenAndAddsCookie() {
    HttpServletResponse response = mock(HttpServletResponse.class);

    recentAuthService.issue(response, 1L);

    verify(recentAuthStore, times(1)).save(any(String.class), eq(1L), any(Instant.class));
    verify(cookieManager, times(1))
        .addCookie(
            eq(response), eq(CookieManager.RECENT_AUTH_TOKEN_NAME), any(String.class), eq(300L));
  }

  @Test
  void validate_SucceedsWithoutExpiringCookie() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    given(cookieManager.getCookie(request, CookieManager.RECENT_AUTH_TOKEN_NAME))
        .willReturn(new Cookie(CookieManager.RECENT_AUTH_TOKEN_NAME, "token"));
    given(recentAuthStore.isValid(eq("token"), eq(1L), any(Instant.class))).willReturn(true);

    recentAuthService.validate(request, 1L);

    verify(cookieManager, never()).expireCookie(response, CookieManager.RECENT_AUTH_TOKEN_NAME);
  }

  @Test
  void validate_ThrowsWhenCookieMissing() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    assertThatThrownBy(() -> recentAuthService.validate(request, 1L))
        .isInstanceOf(AuthException.class);
  }
}
