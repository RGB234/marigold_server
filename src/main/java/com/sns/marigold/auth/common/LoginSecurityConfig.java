package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.common.jwt.JwtAuthenticationFilter;
import com.sns.marigold.auth.oauth2.handler.OAuth2LoginFailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2LoginSuccessHandler;
import com.sns.marigold.auth.oauth2.service.OAuth2UserServiceForLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

/**
 * 로그인 전용 SecurityFilterChain
 * /oauth2/login/** 경로에 적용됩니다.
 */
@Configuration
@RequiredArgsConstructor
@Order(1) // 회원가입보다 먼저 적용
public class LoginSecurityConfig {

  private final CustomCorsConfigurationSource customCorsConfigurationSource;

  private final OAuth2UserServiceForLogin oAuth2UserServiceForLogin;
  private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
  private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final Environment env;

  @Bean
  public SecurityFilterChain loginSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        // 로그인 경로에만 적용
        .securityMatcher(env.getProperty("url.backend.auth.login.base") + "/**") 
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
                .anyRequest().permitAll() // OAuth2 인증은 모두 허용
        )
        .formLogin(AbstractHttpConfigurer::disable)
        // 로그인 전용 OAuth2 설정
        .oauth2Login(
            oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri(env.getProperty("url.backend.auth.login.endpoint")))
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri(env.getProperty("url.backend.auth.login.redirection")))
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler)
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserServiceForLogin)))
        // 예외처리
        .exceptionHandling(
            ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

    return http.build();
  }
}

