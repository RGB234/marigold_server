package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SignupFailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SignupSuccessHandler;
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

/** 회원가입 전용 SecurityFilterChain /oauth2/signup/** 경로에 적용됩니다. */
@Configuration
@RequiredArgsConstructor
@Order(2) // 로그인 다음에 적용
public class SignupSecurityConfig {

  private final CustomCorsConfigurationSource customCorsConfigurationSource;

  //  private final OAuth2UserServiceForSignup oAuth2UserServiceForSignup;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SignupSuccessHandler oAuth2SignupSuccessHandler;
  private final OAuth2SignupFailureHandler oAuth2SignupFailureHandler;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  private final UrlProperties urlProperties;

  @Bean
  public SecurityFilterChain signupSecurityFilterChain(HttpSecurity http) throws Exception {
    String signupBaseUrl = urlProperties.backend().auth().signup().base();
    String signupEndpointBaseUrl = urlProperties.backend().auth().signup().endpoint().base();
    String signupRedirectionUrl = urlProperties.backend().auth().signup().redirection();

    http
        // 회원가입 경로에만 적용
        .securityMatcher(signupBaseUrl + "/**")
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
                    .permitAll() // 회원가입 경로 모두 허용
            )
        .formLogin(AbstractHttpConfigurer::disable)
        // 회원가입 전용 OAuth2 설정
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(endpoint -> endpoint.baseUri(signupEndpointBaseUrl))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri(signupRedirectionUrl))
                    .successHandler(oAuth2SignupSuccessHandler)
                    .failureHandler(oAuth2SignupFailureHandler)
                    //                .userInfoEndpoint(userInfo ->
                    // userInfo.userService(oAuth2UserServiceForSignup)))
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
        // 예외처리
        .exceptionHandling(
            ex ->
                ex.accessDeniedHandler(customAccessDeniedHandler)
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

    return http.build();
  }
}
