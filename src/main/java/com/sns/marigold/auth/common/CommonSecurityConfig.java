package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.common.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
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
// @RequiredArgsConstructor
@Order(3) // 가장 마지막에 적용 (fallback)
public class CommonSecurityConfig {

    // private final Environment env;
    private final CustomCorsConfigurationSource customCorsConfigurationSource;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final String loginBaseUrl;
    private final String signupBaseUrl;
    private final String logoutUrl;
    private final String statusUrl;

    public CommonSecurityConfig(CustomCorsConfigurationSource customCorsConfigurationSource,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${url.backend.auth.login.base}") String loginBaseUrl,
            @Value("${url.backend.auth.signup.base}") String signupBaseUrl,
            @Value("${url.backend.auth.logout}") String logoutUrl,
            @Value("${url.backend.auth.status}") String statusUrl) {

        this.customCorsConfigurationSource = customCorsConfigurationSource;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        this.loginBaseUrl = loginBaseUrl;
        this.signupBaseUrl = signupBaseUrl;
        this.logoutUrl = logoutUrl;
        this.statusUrl = statusUrl;
    }

    @Bean
    public SecurityFilterChain commonSecurityFilterChain(HttpSecurity http) throws Exception {
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
                                // 로그아웃
                                // 인증 상태 반환
                                .requestMatchers(
                                        logoutUrl,
                                        statusUrl)
                                .permitAll()
                                // Swagger
                                .requestMatchers(
                                        "/v3/api-docs/**",
                                        "/swagger-ui/**")
                                .permitAll()
                                .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                // 로그아웃
                // .logout(logout -> logout
                // .logoutUrl(logoutUrl)
                // .permitAll())
                // 예외처리
                .exceptionHandling(
                        ex -> ex
                                .accessDeniedHandler(customAccessDeniedHandler)
                                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));

        return http.build();
    }
}
