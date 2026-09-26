package com.sns.marigold.auth.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.csrf.CsrfTokenService;
import com.sns.marigold.auth.common.csrf.CsrfTokenValidationFilter;
import com.sns.marigold.auth.common.handler.CustomAccessDeniedHandler;
import com.sns.marigold.auth.common.handler.CustomLogoutHandler;
import com.sns.marigold.auth.common.handler.CustomLogoutSuccessHandler;
import com.sns.marigold.auth.common.jwt.JwtAuthenticationFilter;
import com.sns.marigold.auth.common.jwt.JwtProperties;
import com.sns.marigold.auth.common.service.JwtAuthenticationService;
import com.sns.marigold.auth.common.service.RecentAuthService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.config.UrlProperties;
import com.sns.marigold.global.web.UrlConstants;

import jakarta.servlet.http.Cookie;

@SpringJUnitWebConfig(CommonSecurityConfigTest.Config.class)
@TestPropertySource(properties = "url.backend.auth.oauth2.base=/oauth2")
@WithMockUser
class CommonSecurityConfigTest {

  @Autowired private WebApplicationContext context;
  @MockitoBean private CustomCorsConfigurationSource corsConfigurationSource;
  @MockitoBean private CustomAccessDeniedHandler accessDeniedHandler;
  @MockitoBean private CustomAuthenticationEntryPoint authenticationEntryPoint;
  @MockitoBean private RecentAuthService recentAuthService;
  @MockitoBean private JwtAuthenticationService jwtAuthenticationService;
  @MockitoBean private AuditLogger auditLogger;

  private MockMvc mockMvc;

  @ParameterizedTest
  @WithAnonymousUser
  @ValueSource(strings = {"/ws", "/ws/info", "/ws/000/session/websocket"})
  void sockJsHandshake_DoesNotRequireHttpBearerToken(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isNotFound());
    verifyNoInteractions(authenticationEntryPoint);
  }

  @Test
  @WithAnonymousUser
  void refresh_WithCsrf_DoesNotRequireHttpBearerToken() throws Exception {
    mockMvc
        .perform(
            post(UrlConstants.AUTH_BASE + "/refresh")
                .cookie(
                    refreshCookie(), new Cookie(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, "token"))
                .header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, "token"))
        .andExpect(status().isNotFound());
    verifyNoInteractions(authenticationEntryPoint);
  }

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithAnonymousUser
  void request_WithBearerToken_AuthenticatesThroughSecurityChain() throws Exception {
    when(jwtAuthenticationService.getAuthentication("access-token"))
        .thenReturn(new UsernamePasswordAuthenticationToken("jwt-user", null, List.of()));

    mockMvc
        .perform(get("/jwt-filter-test").header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
        .andExpect(status().isNotFound())
        .andExpect(authenticated().withUsername("jwt-user"));

    verify(jwtAuthenticationService).getAuthentication("access-token");
  }

  @Test
  @WithAnonymousUser
  void oauth2Request_WithBearerToken_DoesNotRunJwtFilter() throws Exception {
    mockMvc
        .perform(get("/oauth2/test").header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
        .andExpect(status().isNotFound());

    verifyNoInteractions(jwtAuthenticationService);
  }

  @Test
  void getLogout_DoesNotClearAuthenticationOrCookies() throws Exception {
    mockMvc
        .perform(get(UrlConstants.AUTH_BASE + "/logout").cookie(refreshCookie()))
        .andExpect(status().isNotFound())
        .andExpect(authenticated())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(recentAuthService);
  }

  @Test
  void postLogout_WithMatchingCsrfToken_ClearsAuthenticationAndCookies() throws Exception {
    mockMvc
        .perform(
            post(UrlConstants.AUTH_BASE + "/logout")
                .cookie(
                    refreshCookie(), new Cookie(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, "token"))
                .header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, "token"))
        .andExpect(status().isNoContent())
        .andExpect(unauthenticated())
        .andExpect(cookie().maxAge(CookieManager.REFRESH_TOKEN_NAME, 0))
        .andExpect(cookie().maxAge(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, 0));

    verify(recentAuthService).clear(any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "wrong-token"})
  void postLogout_WithoutMatchingCsrfToken_IsForbidden(String csrfHeader) throws Exception {
    var request =
        post(UrlConstants.AUTH_BASE + "/logout")
            .cookie(refreshCookie(), new Cookie(CsrfTokenService.CSRF_TOKEN_COOKIE_NAME, "token"));
    if (!csrfHeader.isEmpty()) {
      request.header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, csrfHeader);
    }

    mockMvc
        .perform(request)
        .andExpect(status().isForbidden())
        .andExpect(authenticated())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(recentAuthService);
  }

  private Cookie refreshCookie() {
    return new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token");
  }

  @Configuration(proxyBeanMethods = false)
  @EnableWebMvc
  @EnableConfigurationProperties({UrlProperties.class, JwtProperties.class})
  @Import({
    CommonSecurityConfig.class,
    JwtAuthenticationFilter.class,
    CsrfTokenValidationFilter.class,
    CustomLogoutHandler.class,
    CustomLogoutSuccessHandler.class,
    CookieManager.class,
    CsrfTokenService.class
  })
  static class Config {
    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper().findAndRegisterModules();
    }
  }
}
