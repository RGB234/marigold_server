package com.sns.marigold.auth.oauth2;

import com.sns.marigold.auth.common.CustomCorsConfigurationSource;
import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.oauth2.service.CustomOAuth2UserService;
import com.sns.marigold.global.config.UrlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsUtils;

/** 통합 OAuth2 SecurityFilterChain /oauth2/** 경로에 적용됩니다. */
@Configuration
@RequiredArgsConstructor
@Order(1) // 우선 적용
public class OAuth2SecurityConfig {

  private final CustomCorsConfigurationSource customCorsConfigurationSource;

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final OAuth2FailureHandler oAuth2FailureHandler;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  private final UrlProperties urlProperties;

  @Bean
  public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
    String oauth2BaseUrl = urlProperties.backend().auth().oauth2().base();
    String oauth2EndpointBaseUrl = urlProperties.backend().auth().oauth2().endpoint().base();
    String oauth2RedirectionUrl = urlProperties.backend().auth().oauth2().redirection();

    http
        // OAuth2 경로에만 적용
        .securityMatcher(oauth2BaseUrl + "/**")
        // 세션 비활성화 (JWT 사용)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(CorsUtils::isPreFlightRequest)
                    .permitAll()
                    .anyRequest()
                    .permitAll() // OAuth2 경로는 모두 허용 (인증은 소셜 로그인 창에서 이루어짐)
            )
        .formLogin(AbstractHttpConfigurer::disable)
        // 통합 OAuth2 설정
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(endpoint -> endpoint.baseUri(oauth2EndpointBaseUrl))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri(oauth2RedirectionUrl))
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
        // 예외처리
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(customAccessDeniedHandler)
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

    return http.build();
  }
}