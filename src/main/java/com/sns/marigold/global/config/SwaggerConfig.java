package com.sns.marigold.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
public class SwaggerConfig {
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().components(new Components()).info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
        .title("API Test")
        //            .description("Let's practice Swagger UI")
        .version("0.0.0");
  }
}
