package com.sns.marigold.auth.oauth2.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.common.session.SessionTokens;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.config.UrlProperties;

import jakarta.servlet.http.Cookie;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

  @Mock private AuthSessionService authSessionService;
  @Mock private CookieManager cookieManager;
  @Mock private AuditLogger auditLogger;
  @Mock private OAuth2FailureHandler failureHandler;

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
            authSessionService, cookieManager, urlProperties, auditLogger, failureHandler);
  }

  @Test
  @DisplayName("OAuth2 로그인 성공 시 refresh cookie는 세션 만료까지의 초를 쓰고 access token은 URL에 노출하지 않는다.")
  void loginSuccess_IssuesRefreshCookieWithSeconds_AndDoesNotExposeAccessTokenInRedirect()
      throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    given(authSessionService.start(any(CustomPrincipal.class)))
        .willReturn(new SessionTokens("access-token", "refresh-token", 86400L));

    // when
    successHandler.onAuthenticationSuccess(request, response, authentication());

    // then
    verify(cookieManager)
        .addCookie(
            eq(response), eq(CookieManager.REFRESH_TOKEN_NAME), eq("refresh-token"), eq(86400L));

    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:5173/auth/callback?auth_status=LOGIN_SUCCESS");
    assertThat(response.getRedirectedUrl()).doesNotContain("access-token");
    verify(authSessionService, never()).endPrevious(any());
  }

  @Test
  @DisplayName("refresh 쿠키가 있는 채로 다시 로그인하면 새 세션을 만든 뒤 이전 세션을 폐기한다.")
  void loginSuccess_EndsPreviousSession() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    given(cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME))
        .willReturn(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "previous-refresh-token"));
    given(authSessionService.start(any(CustomPrincipal.class)))
        .willReturn(new SessionTokens("access-token", "refresh-token", 86400L));

    successHandler.onAuthenticationSuccess(request, response, authentication());

    InOrder inOrder = inOrder(authSessionService);
    inOrder.verify(authSessionService).start(any(CustomPrincipal.class));
    inOrder.verify(authSessionService).endPrevious("previous-refresh-token");
    verify(cookieManager)
        .addCookie(response, CookieManager.REFRESH_TOKEN_NAME, "refresh-token", 86400L);
  }

  @Test
  @DisplayName("계정 연결은 새 세션을 만들지 않으므로 기존 세션을 그대로 둔다.")
  void linkSuccess_KeepsCurrentSession() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    successHandler.onAuthenticationSuccess(
        request, response, authentication(AuthStatus.LINK_SUCCESS));

    verifyNoInteractions(authSessionService, cookieManager);
    assertThat(response.getRedirectedUrl())
        .isEqualTo("http://localhost:5173/auth/callback?auth_status=LINK_SUCCESS");
  }

  @Test
  @DisplayName("세션 저장소가 응답하지 않으면 쿠키 없이 OAuth2 오류 redirect로 보낸다.")
  void loginSuccess_WhenSessionStoreUnavailable_RedirectsWithError() throws Exception {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    given(authSessionService.start(any(CustomPrincipal.class)))
        .willThrow(AuthException.forSessionStoreUnavailable(new IllegalStateException("down")));

    // when
    successHandler.onAuthenticationSuccess(request, response, authentication());

    // then
    ArgumentCaptor<OAuth2AuthenticationException> exception =
        ArgumentCaptor.forClass(OAuth2AuthenticationException.class);
    verify(failureHandler).onAuthenticationFailure(eq(request), eq(response), exception.capture());
    assertThat(exception.getValue().getError().getErrorCode())
        .isEqualTo(AuthError.SESSION_STORE_UNAVAILABLE.code());
    verifyNoInteractions(cookieManager);
    assertThat(response.getRedirectedUrl()).isNull();
  }

  private Authentication authentication() {
    return authentication(AuthStatus.LOGIN_SUCCESS);
  }

  private Authentication authentication(AuthStatus authStatus) {
    CustomPrincipal principal =
        new CustomPrincipal(
            1L, List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name())), Map.of(), authStatus);
    return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
  }
}
