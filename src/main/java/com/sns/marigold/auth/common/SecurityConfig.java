package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.oauth2.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.SessionFixationConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
  private final Environment env;
  private final CustomCorsConfigurationSource customCorsConfigurationSource;

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final OAuth2FailureHandler oAuth2FailureHandler;

  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  public SecurityConfig(
      Environment env,
      CustomCorsConfigurationSource customCorsConfigurationSource,

      CustomOAuth2UserService customOAuth2UserService,
      OAuth2SuccessHandler oAuth2SuccessHandler,
      OAuth2FailureHandler oAuth2FailureHandler,

      CustomAccessDeniedHandler customAccessDeniedHandler
  ) {
    this.env = env;
    this.customCorsConfigurationSource = customCorsConfigurationSource;

    this.customOAuth2UserService = customOAuth2UserService;
    this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    this.oAuth2FailureHandler = oAuth2FailureHandler;

    this.customAccessDeniedHandler = customAccessDeniedHandler;
  }


  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http
  ) throws Exception {
    http
        // 세선 고정 공격 보호 전략
        .sessionManagement(
            session -> session.sessionFixation(SessionFixationConfigurer::changeSessionId))
        // 프론트엔드/백엔드 다른 포트 or 다른 도메인일 경우 CORS 설정 필요
        .cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable) // 쿠키 사용하지 않으면 꺼도 된다
        .httpBasic(AbstractHttpConfigurer::disable)
        .securityContext((securityContext) -> securityContext.requireExplicitSave(true))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
//            .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/profile").permitAll()
                    // 인증 인가
                    .requestMatchers("/auth/status", "/auth/login/**", "/auth/logout")
                    .permitAll()
                    // read-only
                    .requestMatchers("/adoption/").permitAll()
                    // swagger
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll() // http://localhost:8080/swagger-ui/index.html
                    .anyRequest().authenticated()
//            .hasAnyRole(PERMITTED_ROLES)
        )
        // 폼 로그인 비활성
        .formLogin(AbstractHttpConfigurer::disable)
        // oAuth2 인증 (소셜로그인)
        .oauth2Login(
            oauth2 ->
                oauth2
//            .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorization")) // 기본값 /oauth2/authorization/{registrationId}
//            .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*")) // 기본값
                    .redirectionEndpoint(endpoint -> endpoint.baseUri(
                        "/oauth2/code/*")) // application.yml 파일의 registration.{}.redirect-uri 값과 일치시켜야 함
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
                    // OAuth2 인증 성공 후 사용자 정보를 가져옴
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
        // 로그아웃
        .logout(logout -> logout
            .logoutUrl(env.getProperty("app.url.backend.logout.form"))
            .logoutSuccessUrl(env.getProperty("app.url.frontend.home"))
            .permitAll()
        )
        // 예외처리
        .exceptionHandling(
            ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler)
                // 인증 실패시 401 코드 반환 (기본값 : 인증화면으로 리다이렉션)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

    return http.build();
  }

  @Bean
  public SecurityContextRepository securityContextRepository() {
    // **주의**: 이 Bean을 정의하지 않고 Spring Security의 기본 설정을 따르게 할 수도 있지만,
    // CustomSuccessHandler에 명시적으로 주입하려면 이렇게 정의해야 합니다.
    return new HttpSessionSecurityContextRepository();
  }

}
