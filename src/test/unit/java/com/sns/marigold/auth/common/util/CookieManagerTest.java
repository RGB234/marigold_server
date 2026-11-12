package com.sns.marigold.auth.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieManagerTest {

  private final CookieManager cookieManager = new CookieManager();

  @Test
  void authCookiesAreSameSiteLax_SoCrossSitePostsDoNotCarryThem() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    cookieManager.addCookie(response, CookieManager.REFRESH_TOKEN_NAME, "token", 60);
    cookieManager.expireCookie(response, CookieManager.REFRESH_TOKEN_NAME);

    assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
        .hasSize(2)
        .allSatisfy(
            header ->
                assertThat(header)
                    .contains("HttpOnly")
                    .contains("Secure")
                    .contains("SameSite=Lax"));
  }
}
