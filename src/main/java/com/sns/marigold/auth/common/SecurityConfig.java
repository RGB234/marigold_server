package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.form.CustomUsernamePasswordAuthenticationFilter;
import com.sns.marigold.auth.form.service.CustomUserDetailsService;
import com.sns.marigold.auth.oauth2.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.oauth2.service.CustomOAuth2UserService;
import com.sns.marigold.global.config.CustomCorsConfigurationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer.SessionFixationConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

//  private static final String[] PERMITTED_ROLES =
//    Arrays.stream(Role.values()).map(Role::name).toArray(String[]::new);

  private static final String[] PERMITTED_ROLES = {"ROLE_ADMIN", "ROLE_PERSON", "ROLE_INSTITUTION"};

  private final Environment env;
  private final PasswordEncoder passwordEncoder;
  private final CustomCorsConfigurationSource customCorsConfigurationSource;

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final OAuth2FailureHandler oAuth2FailureHandler;

  private final CustomUserDetailsService customUserDetailsService;

  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  public SecurityConfig(
      Environment env,
      PasswordEncoder passwordEncoder,
      CustomCorsConfigurationSource customCorsConfigurationSource,

      CustomOAuth2UserService customOAuth2UserService,
      OAuth2SuccessHandler oAuth2SuccessHandler,
      OAuth2FailureHandler oAuth2FailureHandler,

      CustomUserDetailsService customUserDetailsService,

      CustomAccessDeniedHandler customAccessDeniedHandler
  ) {
    this.env = env;
    this.passwordEncoder = passwordEncoder;
    this.customCorsConfigurationSource = customCorsConfigurationSource;

    this.customOAuth2UserService = customOAuth2UserService;
    this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    this.oAuth2FailureHandler = oAuth2FailureHandler;

    this.customUserDetailsService = customUserDetailsService;

    this.customAccessDeniedHandler = customAccessDeniedHandler;
  }


  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CustomUsernamePasswordAuthenticationFilter customAuthenticationFilter
  ) throws Exception {
    http
        // 세선 고정 공격 보호 전략
        .sessionManagement(
            session -> session.sessionFixation(SessionFixationConfigurer::changeSessionId))
        // 프론트엔드/백엔드 다른 포트 or 다른 도메인일 경우 CORS 설정 필요
        .cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable) // 쿠키 사용하지 않으면 꺼도 된다
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
//            .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/profile").permitAll()
                    // 임시
//            .requestMatchers("/login").permitAll()
                    // 인증 인가
                    .requestMatchers("/auth/status", "/auth/login/**", "/auth/logout")
                    .permitAll()
                    // 게정 생성
                    .requestMatchers("/user/create/institution").permitAll()
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
        // 로그인
        // username/pw 인증
        .addFilterAt(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
//          .accessDeniedHandler(customAccessDeniedHandler)
                // 인증 실패시 401 코드 반환 (기본값 : 인증화면으로 리다이렉션)
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
    // http.getSharedObject()를 통해 빌더 객체를 가져옵니다.
    AuthenticationManagerBuilder authManagerBuilder =
        http.getSharedObject(AuthenticationManagerBuilder.class);

    // UserDetailsService와 PasswordEncoder를 수동으로 연결합니다.
    authManagerBuilder
        .userDetailsService(customUserDetailsService)
        .passwordEncoder(passwordEncoder);

    // 최종적으로 AuthenticationManager를 빌드하여 반환합니다.
    return authManagerBuilder.build();
  }

  @Bean
  public CustomUsernamePasswordAuthenticationFilter customAuthenticationFilter(AuthenticationManager authManager) {
    CustomUsernamePasswordAuthenticationFilter customFilter = new CustomUsernamePasswordAuthenticationFilter(
        authManager);

    customFilter.setFilterProcessesUrl("/api/login");

    return customFilter;
  }
}
