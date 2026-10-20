package com.sns.marigold.auth.common.csrf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.common.util.CookieManager;

class CsrfTokenServiceTest {

  private CsrfTokenService csrfTokenService;

  @BeforeEach
  void setUp() {
    csrfTokenService =
        new CsrfTokenService(
            new CookieManager(),
            new JwtProperties("j".repeat(32), 600, 86400),
            new CsrfProperties("c".repeat(32)));
  }

  @Test
  void issuedToken_IsValidOnlyForItsSessionBinding() {
    String token = issue("refresh-token");

    assertThat(csrfTokenService.isValid(token, token, "refresh-token")).isTrue();
    assertThat(csrfTokenService.isValid(token, token, "other-refresh-token")).isFalse();
  }

  @Test
  void tamperedOrMismatchedToken_IsInvalid() {
    String token = issue("refresh-token");
    String tamperedToken = "A" + token.substring(1);

    assertThat(csrfTokenService.isValid(token, tamperedToken, "refresh-token")).isFalse();
    assertThat(csrfTokenService.isValid(tamperedToken, tamperedToken, "refresh-token")).isFalse();
    assertThat(csrfTokenService.isValid("invalid", "invalid", "refresh-token")).isFalse();
  }

  @Test
  void consecutiveIssues_UseDifferentRandomValues() {
    assertThat(issue("refresh-token")).isNotEqualTo(issue("refresh-token"));
  }

  private String issue(String sessionBinding) {
    MockHttpServletResponse response = new MockHttpServletResponse();
    csrfTokenService.issue(response, sessionBinding);
    return response.getHeader(CsrfTokenService.CSRF_TOKEN_HEADER_NAME);
  }
}
