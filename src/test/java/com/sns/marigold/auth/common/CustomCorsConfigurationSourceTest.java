package com.sns.marigold.auth.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import com.sns.marigold.global.config.UrlProperties;

class CustomCorsConfigurationSourceTest {

  @Test
  void usesFrontendOriginWithoutPath() {
    CustomCorsConfigurationSource source =
        new CustomCorsConfigurationSource(urlProperties("http://localhost:8000"));

    CorsConfiguration config =
        source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/adoption"));

    assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:8000");
  }

  @Test
  void allowsOptionsMethodForPreflight() {
    CustomCorsConfigurationSource source =
        new CustomCorsConfigurationSource(urlProperties("http://localhost:8000"));

    CorsConfiguration config =
        source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/adoption"));

    assertThat(config.getAllowedMethods()).contains("OPTIONS");
  }

  private UrlProperties urlProperties(String frontendOrigin) {
    return new UrlProperties(new UrlProperties.Frontend(frontendOrigin, null), null);
  }
}
