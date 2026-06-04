package com.sns.marigold.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.sns.marigold.auth.common.csrf.CsrfTokenService;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Profile("dev")
@Configuration
public class SwaggerConfig {
  public static final String BEARER_AUTH = "bearerAuth";
  public static final String CSRF_TOKEN = "csrfToken";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().components(components()).info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
        .title("Marigold API")
        .description("Marigold backend API documentation")
        .version("0.0.1");
  }

  private Components components() {
    return new Components()
        .addSecuritySchemes(
            BEARER_AUTH,
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"))
        .addSecuritySchemes(
            CSRF_TOKEN,
            new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(CsrfTokenService.CSRF_TOKEN_HEADER_NAME));
  }
}
