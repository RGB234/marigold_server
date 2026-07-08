package com.sns.marigold.auth.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsUtils;

import com.sns.marigold.auth.common.CustomAuthenticationEntryPoint;
import com.sns.marigold.auth.common.CustomCorsConfigurationSource;
import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.oauth2.service.CustomOAuth2UserService;
import com.sns.marigold.global.config.UrlProperties;

import lombok.RequiredArgsConstructor;

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
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final HttpCookieOAuth2AuthorizationRequestRepository
      httpCookieOAuth2AuthorizationRequestRepository;

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
        //        .addFilterBefore(csrfTokenValidationFilter, LogoutFilter.class)
        // 통합 OAuth2 설정
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(
                        endpoint ->
                            endpoint
                                .baseUri(oauth2EndpointBaseUrl)
                                .authorizationRequestRepository(
                                    httpCookieOAuth2AuthorizationRequestRepository))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri(oauth2RedirectionUrl))
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
        //    OAuth2 인증 과정의 성공/실패는 successHandler/failureHandler가 처리한다.
        //    exceptionHandling은 현재 메인 OAuth2 플로우에서 자주 호출되지는 않지만,
        //    OAuth2 필터 체인 내에서 인증/인가 예외가 발생할 경우의 fallback 응답 정책을 명시한다
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(customAccessDeniedHandler) // 403
                    .authenticationEntryPoint(customAuthenticationEntryPoint)); // 401

    return http.build();
  }
}
