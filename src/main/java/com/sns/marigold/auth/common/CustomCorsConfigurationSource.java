package com.sns.marigold.auth.common;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.sns.marigold.auth.common.csrf.CsrfTokenService;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CustomCorsConfigurationSource implements CorsConfigurationSource {

  private final String ALLOWED_ORIGIN;
  private final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PATCH", "OPTIONS", "DELETE");

  public CustomCorsConfigurationSource(@Value("${url.frontend.origin}") String frontendOrigin) {
    URI frontendUri = URI.create(frontendOrigin);
    ALLOWED_ORIGIN = frontendUri.getScheme() + "://" + frontendUri.getAuthority();
  }

  @Override
  @Nullable
  public CorsConfiguration getCorsConfiguration(@NonNull HttpServletRequest request) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Collections.singletonList(ALLOWED_ORIGIN));
    config.setAllowedMethods(ALLOWED_METHODS);
    config.setAllowCredentials(true);
    //
    config.setAllowedHeaders(Collections.singletonList("*"));
    config.setExposedHeaders(Collections.singletonList(CsrfTokenService.CSRF_TOKEN_HEADER_NAME));
    config.setMaxAge(3600L); // 1h
    return config;
  }
}
