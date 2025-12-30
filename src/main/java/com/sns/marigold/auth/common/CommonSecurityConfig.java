package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.common.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

/**
 * 일반 API 요청을 위한 SecurityFilterChain
 * OAuth2 경로를 제외한 모든 경로에 적용됩니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Order(3) // 가장 마지막에 적용 (fallback)
public class CommonSecurityConfig {

  private final Environment env;
  private final CustomCorsConfigurationSource customCorsConfigurationSource;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain commonSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        // OAuth2 경로를 제외한 모든 경로에 적용
        .securityMatcher(request -> {
          String path = request.getRequestURI();
          // OAuth2 로그인/회원가입 경로는 제외
          return !path.startsWith(env.getProperty("url.backend.auth.login.base")) &&
                 !path.startsWith(env.getProperty("url.backend.auth.signup.base"));
        })
        // 세션 비활성화 (JWT 사용)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        // JWT 인증 필터
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth -> auth
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                // 인증 인가 - OAuth2 엔드포인트
                .requestMatchers(
                    env.getProperty("url.backend.auth.login.endpoint") + "/**",
                    env.getProperty("url.backend.auth.signup.endpoint") + "/**"
                    // "/oauth2/login/authorization/**",
                    // "/oauth2/signup/authorization/**"
                ).permitAll()
                // 로그아웃, 상태 확인
                .requestMatchers(
                    env.getProperty("url.backend.auth.logout"),
                    env.getProperty("url.backend.auth.status"))
                .permitAll()
                // Swagger
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**")
                .permitAll()
                .anyRequest().authenticated()
        )
        .formLogin(AbstractHttpConfigurer::disable)
        // 로그아웃
        .logout(logout -> logout
            .logoutUrl(env.getProperty("url.backend.auth.logout"))
            .permitAll())
        // 예외처리
        .exceptionHandling(
            ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

    return http.build();
  }
}

