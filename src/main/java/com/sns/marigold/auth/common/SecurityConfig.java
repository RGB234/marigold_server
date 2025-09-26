package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.form.CustomUsernamePasswordAuthenticationFilter;
import com.sns.marigold.auth.form.service.CustomUserDetailService;
import com.sns.marigold.auth.oauth2.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.oauth2.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.oauth2.service.CustomOAuth2UserService;
import com.sns.marigold.global.config.CustomCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsUtils;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

//  private static final String[] PERMITTED_ROLES =
//    Arrays.stream(Role.values()).map(Role::name).toArray(String[]::new);

  private static final String[] PERMITTED_ROLES = {"ROLE_ADMIN", "ROLE_PERSON", "ROLE_INSTITUTION",
    "ROLE_NOT_REGISTERED"};

  private final Environment env;
  private final CustomCorsConfigurationSource customCorsConfigurationSource;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler successHandler;
  private final OAuth2FailureHandler failureHandler;
  private final PasswordEncoder passwordEncoder;
  //
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
//  private final CustomUserDetailService customUserDetailService;

  public SecurityConfig(
    Environment env,
    CustomCorsConfigurationSource customCorsConfigurationSource,
    CustomOAuth2UserService customOAuth2UserService,
    OAuth2SuccessHandler oAuth2SuccessHandler,
    OAuth2FailureHandler oAuth2FailureHandler,
    PasswordEncoder passwordEncoder,
    //
    CustomAccessDeniedHandler customAccessDeniedHandler,
    CustomUserDetailService customUserDetailService
  ) {
    this.env = env;
    this.customCorsConfigurationSource = customCorsConfigurationSource;
    this.customOAuth2UserService = customOAuth2UserService;
    this.successHandler = oAuth2SuccessHandler;
    this.failureHandler = oAuth2FailureHandler;
    this.passwordEncoder = passwordEncoder;
    //
    this.customAccessDeniedHandler = customAccessDeniedHandler;
//    this.customUserDetailService = customUserDetailService;
  }

  // 인증 방식
  // 1.Http basic -> 사용안함
  // 매 요청마다 헤더에 username:password 전달 인증방식, Stateless(Session X)
  // 최초 인증 성공 후 브라우저가 Authorization 헤더를 추가
  // 2.Form 로그인 -> 사용 (Email/Password)
  // 3.OAuth2 -> 사용 (소셜로그인)

  @Bean
  public SecurityFilterChain securityFilterChain(
    HttpSecurity http,
    AuthenticationManager authManager
  ) throws Exception {
    CustomUsernamePasswordAuthenticationFilter customFilter = new CustomUsernamePasswordAuthenticationFilter(
      authManager);
//    customFilter.setFilterProcessesUrl("/api/login"); // 로그인 엔드포인트 변경 가능
//    customFilter.setFilterProcessesUrl(env.getProperty("app.url.backend.login.institution"));

    http
      // 세선 고정 공격 보호 전략
      .sessionManagement(
        session -> session.sessionFixation(SessionFixationConfigurer::changeSessionId))
      // 프론트엔드/백엔드 다른 포트 or 다른 도메인일 경우 CORS 설정 필요
      .cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
      .csrf(AbstractHttpConfigurer::disable) // 쿠키 사용하지 않으면 꺼도 된다
      .httpBasic(AbstractHttpConfigurer::disable)
      // 폼 로그인
//      .formLogin(AbstractHttpConfigurer::disable)
      // 폼 로그아웃
//      .logout(AbstractHttpConfigurer::disable)
      .formLogin(form -> form
          // 입력 폼
//        .loginPage("/login")
          // 입력 폼에서 작성한 데이터 UsernamePasswordAuthenticationFilter 가 가로채서 인증 처리
          .loginProcessingUrl(env.getProperty("app.url.backend.login.form"))
          .successHandler(successHandler())
          .failureHandler(failureHandler())
//          .defaultSuccessUrl() // 로그인 성공 시 이동
//          .failureUrl(env.getProperty("app.url.frontend.login")) // 실패 시 이동
          .permitAll()
      )
      .logout(logout -> logout
        .logoutUrl(env.getProperty("app.url.backend.logout.institution"))
        .logoutSuccessUrl(env.getProperty("app.url.frontend.home"))
        .permitAll()
      )
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
            .requestMatchers("/swagger-ui/**").permitAll()
            .anyRequest().authenticated()
//            .hasAnyRole(PERMITTED_ROLES)
      )
      .oauth2Login(
        oauth2 ->
          oauth2
//            .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorization")) // 기본값 /oauth2/authorization/{registrationId}
//            .redirectionEndpoint(endpoint -> endpoint.baseUri("/login/oauth2/code/*")) // 기본값
            .redirectionEndpoint(endpoint -> endpoint.baseUri(
              "/oauth2/code/*")) // registration.{}.redirect-uri 값과 맞춰야 함
            .successHandler(successHandler)
            .failureHandler(failureHandler)
            // OAuth2 인증 성공 후 사용자 정보를 가져올 때 설정
            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
      .exceptionHandling(
        ex -> ex
//          .accessDeniedHandler(customAccessDeniedHandler)
          // 인증 실패시 401 코드 반환 (기본값 : 인증화면으로 리다이렉션)
          .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
      )
      .addFilter(customFilter)
    ;

    return http.build();
  }

  @Bean
  public AuthenticationManager authManager(HttpSecurity http,
    CustomUserDetailService customUserDetailService) throws Exception {
    AuthenticationManagerBuilder authManagerBuilder =
      http.getSharedObject(AuthenticationManagerBuilder.class);

    authManagerBuilder
      .userDetailsService(customUserDetailService)
      .passwordEncoder(passwordEncoder);

    return authManagerBuilder.build();
  }

  @Bean
  public AuthenticationSuccessHandler successHandler() {
    return (request, response, authentication) -> {
      response.setStatus(HttpServletResponse.SC_OK);
      response.setContentType("application/json; charset=UTF-8");
      // 캐시 방지 헤더 추가
      response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      response.setHeader("Pragma", "no-cache"); // HTTP/1.0 표준
      response.setHeader("Expires", "0");

      response.getWriter().write("{\"message\": \"login success\"}");
    };
  }

  @Bean
  public AuthenticationFailureHandler failureHandler() {
    return (request, response, exception) -> {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json; charset=UTF-8");
      // 캐시 방지 헤더 추가
      response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
      response.setHeader("Pragma", "no-cache"); // HTTP/1.0 표준
      response.setHeader("Expires", "0");

      String jsonResponse = """
        {
              "status": 401,
              "error": "Unauthorized",
              "message": "Invalid email or password."
        }
        """;
      response.getWriter().write(jsonResponse);
    };
  }
}
