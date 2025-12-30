package com.sns.marigold.auth.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Component
public class CustomCorsConfigurationSource implements CorsConfigurationSource {

  private final String ALLOWED_ORIGIN;
  private final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PATCH", "OPTION", "DELETE");

  public CustomCorsConfigurationSource(@Value("${url.frontend.domain}") String baseUrl) {
    ALLOWED_ORIGIN = baseUrl;
  }

  @Override
  public CorsConfiguration getCorsConfiguration(@NonNull HttpServletRequest request) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Collections.singletonList(ALLOWED_ORIGIN));
    config.setAllowedMethods(ALLOWED_METHODS);
    config.setAllowCredentials(true);
    //
    config.setAllowedHeaders(Collections.singletonList("*"));
    config.setMaxAge(3600L); // 1h
    return config;
  }
}
