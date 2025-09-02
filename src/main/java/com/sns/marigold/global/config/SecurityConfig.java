package com.sns.marigold.global.config;

import com.sns.marigold.auth.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.handler.OAuth2FailureHandler;
import com.sns.marigold.auth.handler.OAuth2SuccessHandler;
import com.sns.marigold.auth.service.CustomOAuth2UserService;
import com.sns.marigold.global.enums.Role;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsUtils;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  private static final String[] PERMITTED_ROLES =
      Arrays.stream(Role.values()).map(Role::getValue).toArray(String[]::new);

  //    private static final String[] PERMITTED_ROLES = {"ADMIN", "USER", "NOT_REGISTERED"};

  private final CustomCorsConfigurationSource customCorsConfigurationSource;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler successHandler;
  private final OAuth2FailureHandler failureHandler;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(corsCustomizer -> corsCustomizer.configurationSource(customCorsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        // httpBasic 인증 비활성화 (OAuth 사용)
        .httpBasic(AbstractHttpConfigurer::disable)
        // 기본 로그인 비활성화 (OAuth 사용)
        .formLogin(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(CorsUtils::isPreFlightRequest)
                    .permitAll()
                    .requestMatchers("/", "/css/**", "/images/**", "/js/**", "/profile")
                    .permitAll()
                    // dev
                    .requestMatchers("/swagger-ui/index.html")
                    .hasRole(Role.ROLE_ADMIN.getValue())
                    .anyRequest()
                    .hasAnyRole(PERMITTED_ROLES)
            //                .anyRequest().hasAnyAuthority(PERMITTED_ROLES) // ROLE_ prefix is not
            // applied
            )
        .oauth2Login(
            oauth2 ->
                oauth2
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
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
