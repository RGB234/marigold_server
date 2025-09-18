package com.sns.marigold.auth;

import com.sns.marigold.auth.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.service.CustomOAuth2UserService;
import com.sns.marigold.global.config.CustomCorsConfigurationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.SessionFixationConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsUtils;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

//  private static final String[] PERMITTED_ROLES =
//    Arrays.stream(Role.values()).map(Role::name).toArray(String[]::new);

  private static final String[] PERMITTED_ROLES = {"ROLE_ADMIN", "ROLE_PERSON", "ROLE_INSTITUTION",
    "ROLE_NOT_REGISTERED"};

  private final CustomCorsConfigurationSource customCorsConfigurationSource;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler successHandler;
  private final OAuth2FailureHandler failureHandler;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      // 세선 고정 공격 보호 전략
      .sessionManagement(
        session -> session.sessionFixation(SessionFixationConfigurer::changeSessionId))
      // 프론트엔드/백엔드 다른 포트 or 다른 도메인일 경우 CORS 설정 필요
      .cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
      .csrf(AbstractHttpConfigurer::disable) // 쿠키 사용하지 않으므로 꺼도 된다
      // 기본 인증 로그인 비활성화
      // 일단은 oauth2Login 까지만 구현할 것이므로 비활성.
      // 추후 email/username 로그인도 구현할 때 활성화 할 예정
      .httpBasic(AbstractHttpConfigurer::disable)
      // 기본 로그인 폼 비활성화
      .formLogin(AbstractHttpConfigurer::disable)
      // 기본 로그아웃 폼 비활성화
      .logout(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(
        auth ->
          auth.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
//            .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/profile").permitAll()
            // 인증 API
            .requestMatchers("/api/auth/check-auth", "/api/auth/login", "/api/auth/logout")
            .permitAll()
            .requestMatchers("/v3/api-docs/**").permitAll() // swagger
            .requestMatchers("/swagger-ui/**").permitAll() // swagger
            .anyRequest().authenticated()
//            .hasAnyRole(PERMITTED_ROLES)
      )
      .oauth2Login(
        oauth2 ->
          oauth2
            .successHandler(successHandler)
            .failureHandler(failureHandler)
            // OAuth2 인증 성공 후 사용자 정보를 가져올 때 설정
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)));
    //            .exceptionHandling(
    //                ex -> ex
    //                    .accessDeniedHandler(customAccessDeniedHandler)
    //            )

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12); // with strength 12. default 10
  }
}
