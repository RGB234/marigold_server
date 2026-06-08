package com.sns.marigold.auth.oauth2.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.csrf.CsrfTokenService;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.service.RecentAuthService;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sns.marigold.global.config.UrlProperties;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

  @Mock private JwtManager jwtManager;
  @Mock private CookieManager cookieManager;
  @Mock private CsrfTokenService csrfTokenService;
  @Mock private RecentAuthService recentAuthService;
  @Mock private HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
  @Mock private AuditLogger auditLogger;

  private OAuth2SuccessHandler successHandler;

  @BeforeEach
  void setUp() {
    UrlProperties urlProperties =
        new UrlProperties(
            new UrlProperties.Frontend(
                "http://localhost:5173",
                new UrlProperties.Frontend.Auth(
                    "http://localhost:5173/login",
                    "http://localhost:5173/signup",
                    "http://localhost:5173/auth/callback")),
            null);

    successHandler =
        new OAuth2SuccessHandler(
            jwtManager,
            cookieManager,
            csrfTokenService,
            recentAuthService,
            urlProperties,
            auditLogger,
            authorizationRequestRepository);
  }

  @Test
  @DisplayName("OAuth2 로그인 성공 시 refresh cookie는 초 단위 만료값을 사용하고 access token은 URL에 노출하지 않는다.")
  void loginSuccess_IssuesRefreshCookieWithSeconds_AndDoesNotExposeAccessTokenInRedirect()
      throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    CustomPrincipal principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name())),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    given(jwtManager.createRefreshToken(any(CustomPrincipal.class))).willReturn("refresh-token");
    given(jwtManager.getRefreshTokenValidityInSeconds()).willReturn(86400L);

    // when
    successHandler.onAuthenticationSuccess(request, response, authentication);

    // then
    verify(cookieManager)
        .addCookie(
            eq(response), eq(CookieManager.REFRESH_TOKEN_NAME), eq("refresh-token"), eq(86400L));
    verify(recentAuthService).issue(response, 1L);
    verify(csrfTokenService).issue(response);
    verify(jwtManager, never()).createAccessToken(any(CustomPrincipal.class));

    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:5173/auth/callback?auth_status=LOGIN_SUCCESS");
    assertThat(response.getRedirectedUrl()).doesNotContain("access_token");
  }
}
