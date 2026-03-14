package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.common.jwt.JwtAuthenticationFilter;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.config.UrlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

/**
 * 일반 API 요청을 위한 SecurityFilterChain OAuth2 경로를 제외한 모든 경로에 적용됩니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Order(3) // SecurityConfig중에서 가장 마지막에 적용 (fallback)
public class CommonSecurityConfig {

  private final CustomCorsConfigurationSource customCorsConfigurationSource;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final UrlProperties urlProperties;

  @Bean
  public SecurityFilterChain commonSecurityFilterChain(HttpSecurity http) throws Exception {
    String loginBaseUrl = urlProperties.backend().auth().login().base();
    String signupBaseUrl = urlProperties.backend().auth().signup().base();

    http
        .securityMatcher(request -> {
          String path = request.getRequestURI();
          // OAuth2 로그인/회원가입 경로는 제외
          return !path.startsWith(loginBaseUrl) &&
              !path.startsWith(signupBaseUrl);
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
                // Swagger
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**")
                .permitAll()
                // 구체적인 권한 제어는 Controller의 @PreAuthorize에서 처리하므로
                // 필터 체인 레벨에서는 모든 요청을 통과.
                .anyRequest().permitAll())
        .formLogin(AbstractHttpConfigurer::disable)
        // 지금은 AuthController에서 처리 중
//        .logout(logout -> logout
//            .logoutUrl("/api/auth/logout")
//            .addLogoutHandler(customLogoutHandler) // 토큰 블랙리스트 처리 등
//            .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)) // 성공 시 200 반환
//        )
        .exceptionHandling(
            ex -> ex
                .accessDeniedHandler(customAccessDeniedHandler) // 403 권한없음
                .authenticationEntryPoint(customAuthenticationEntryPoint)); // 401 인증실패

    return http.build();
  }
}
