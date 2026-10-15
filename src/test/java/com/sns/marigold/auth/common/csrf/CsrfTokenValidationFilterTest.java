package com.sns.marigold.auth.common.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;

class CsrfTokenValidationFilterTest {

  private CsrfTokenValidationFilter filter;
  private FilterChain filterChain;
  private CsrfTokenService csrfTokenService;

  @BeforeEach
  void setUp() {
    CookieManager cookieManager = new CookieManager();
    csrfTokenService =
        new CsrfTokenService(
            cookieManager,
            new JwtProperties("j".repeat(32), 600, 86400),
            new CsrfProperties("c".repeat(32)));
    filter =
        new CsrfTokenValidationFilter(
            cookieManager,
            csrfTokenService,
            new ProblemDetailWriter(new ObjectMapper().findAndRegisterModules()));
    filterChain = Mockito.mock(FilterChain.class);
  }

  @Test
  void unsafeRequestWithCookieAuthAndMatchingToken_Passes() throws Exception {
    String csrfToken = issueToken("refresh-token");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(
        new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"),
        new Cookie(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, csrfToken));
    request.addHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, csrfToken);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void unsafeRequestWithTokenFromAnotherSession_IsForbidden() throws Exception {
    String csrfToken = issueToken("other-refresh-token");
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(
        new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"),
        new Cookie(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, csrfToken));
    request.addHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, csrfToken);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(403);
    verify(filterChain, never()).doFilter(request, response);
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

  @Test
  void unsafeSockJsRequestWithCookieAuth_PassesWithoutToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/ws/123/abc/xhr_send");
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setCookies(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"));

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  private String issueToken(String sessionBinding) {
    MockHttpServletResponse response = new MockHttpServletResponse();
    csrfTokenService.issue(response, sessionBinding);
    return response.getHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME);
  }
}
