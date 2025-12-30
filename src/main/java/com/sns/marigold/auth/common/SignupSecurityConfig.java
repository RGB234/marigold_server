package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.common.jwt.JwtAuthenticationFilter;
import com.sns.marigold.auth.oauth2.handler.OAuth2SignupFailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SignupSuccessHandler;
import com.sns.marigold.auth.oauth2.service.OAuth2UserServiceForSignup;
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
 * 회원가입 전용 SecurityFilterChain
 * /oauth2/signup/** 경로에 적용됩니다.
 */
@Configuration
@RequiredArgsConstructor
@Order(2) // 로그인 다음에 적용
public class SignupSecurityConfig {

  private final CustomCorsConfigurationSource customCorsConfigurationSource;

  private final OAuth2UserServiceForSignup oAuth2UserServiceForSignup;
  private final OAuth2SignupSuccessHandler oAuth2SignupSuccessHandler;
  private final OAuth2SignupFailureHandler oAuth2SignupFailureHandler;

  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final Environment env;

  @Bean
  public SecurityFilterChain signupSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        // 회원가입 경로에만 적용 (authorization + callback 모두 포함)
        .securityMatcher(env.getProperty("url.backend.auth.signup.base") + "/**")
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
        // 회원가입 전용 OAuth2 설정
        .oauth2Login(
            oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri(env.getProperty("url.backend.auth.signup.endpoint")))
                    // .baseUri("/oauth2/signup/authorization"))
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri(env.getProperty("url.backend.auth.signup.redirection")))
                    // .baseUri("/oauth2/signup/code/*"))
                .successHandler(oAuth2SignupSuccessHandler)
                .failureHandler(oAuth2SignupFailureHandler)
                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserServiceForSignup)))
        // 예외처리
        .exceptionHandling(
            ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

    return http.build();
  }
}

