package com.sns.marigold.auth.common.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.util.CookieManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CsrfTokenValidationFilterTest {

  private CsrfTokenValidationFilter filter;
  private FilterChain filterChain;

  @BeforeEach
  void setUp() {
    filter =
        new CsrfTokenValidationFilter(
            new CookieManager(), new ObjectMapper().findAndRegisterModules());
    filterChain = Mockito.mock(FilterChain.class);
  }

  @Test
  void unsafeRequestWithCookieAuthAndMatchingToken_Passes() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(
        new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"),
        new Cookie(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, "csrf-token"));
    request.addHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, "csrf-token");

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void unsafeRequestWithCookieAuthAndMissingToken_IsForbidden() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"));

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(filterChain, never()).doFilter(request, response);
  }

  @Test
  void safeRequestWithCookieAuth_PassesWithoutToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/status");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }
}
