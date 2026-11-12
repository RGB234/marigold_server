package com.sns.marigold.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.support.ApiIntegrationTest;
import com.sns.marigold.user.entity.User;

import jakarta.servlet.http.Cookie;

/** 로그인부터 refresh, 로그아웃, 탈퇴까지 Redis 세션이 실제 HTTP 흐름에서 지켜지는지 확인합니다. */
class AuthSessionApiIntegrationTest extends ApiIntegrationTest {

  private static final String EMAIL = "session@example.com";
  private static final String PASSWORD = "password123!";

  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUpLocalCredentials() {
    User user = Objects.requireNonNull(tester1);
    user.addEmailAndPassword(EMAIL, passwordEncoder.encode(PASSWORD));
    userRepository.save(user);
  }

  @Test
  @DisplayName("refresh는 token을 회전하고, 로그아웃하면 refresh와 access token 모두 바로 거부된다")
  void logoutRevokesBothTokens() throws Exception {
    Session login = login();
    Session refreshed = session(refresh(login).andExpect(status().isOk()).andReturn());
    assertThat(refreshed.refreshCookie().getValue()).isNotEqualTo(login.refreshCookie().getValue());
    me(refreshed).andExpect(status().isOk());

    mockMvc
        .perform(post(UrlConstants.AUTH_BASE + "/logout").cookie(refreshed.refreshCookie()))
        .andExpect(status().isNoContent());

    refresh(refreshed)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
    me(refreshed)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
  }

  @Test
  @DisplayName("refresh 쿠키는 claim이 없는 불투명 값이고, 형식이 다른 값은 거부한다")
  void refreshCookieIsOpaque() throws Exception {
    Session login = login();

    // JWT라면 점이 두 개다. 지금은 {sid}.{비밀값} 하나뿐이다
    assertThat(login.refreshCookie().getValue().chars().filter(c -> c == '.')).hasSize(1);
    refresh(new Session(login.accessToken(), new Cookie(CookieManager.REFRESH_TOKEN_NAME, "a.b.c")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
  }

  @Test
  @DisplayName("응답을 못 받은 직후의 재시도는 유예 안에서 허용한다")
  void retryWithPreviousTokenWithinGraceSucceeds() throws Exception {
    Session login = login();
    refresh(login).andExpect(status().isOk());

    Session retried = session(refresh(login).andExpect(status().isOk()).andReturn());

    refresh(retried).andExpect(status().isOk());
  }

  @Test
  @DisplayName("이미 두 번 회전된 refresh token이 다시 오면 세션 전체를 폐기한다")
  void reusedRefreshTokenRevokesSession() throws Exception {
    Session login = login();
    Session second = session(refresh(login).andExpect(status().isOk()).andReturn());
    Session third = session(refresh(second).andExpect(status().isOk()).andReturn());

    refresh(login).andExpect(status().isUnauthorized());

    refresh(third).andExpect(status().isUnauthorized());
    me(third).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("방금 로그인한 세션으로 탈퇴하면 다른 기기의 세션 access token도 바로 거부된다")
  void deletingUserRevokesAllSessions() throws Exception {
    Session otherDevice = login();
    me(otherDevice).andExpect(status().isOk());

    Session deletingDevice = login();
    mockMvc
        .perform(
            delete(UrlConstants.USER_BASE + "/delete")
                .header("Authorization", "Bearer " + deletingDevice.accessToken()))
        .andExpect(status().isNoContent());

    me(otherDevice)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
  }

  @Test
  @DisplayName("refresh 쿠키가 있는 채로 다시 로그인하면 이전 세션은 바로 폐기되고 다른 기기의 세션은 남는다")
  void loggingInAgainRevokesPreviousSessionOfTheSameBrowser() throws Exception {
    Session otherDevice = login();
    Session previous = login();

    Session current =
        session(
            mockMvc
                .perform(loginRequest().cookie(previous.refreshCookie()))
                .andExpect(status().isOk())
                .andReturn());

    refresh(previous)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
    me(previous)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
    me(current).andExpect(status().isOk());
    me(otherDevice).andExpect(status().isOk());
  }

  private Session login() throws Exception {
    return session(mockMvc.perform(loginRequest()).andExpect(status().isOk()).andReturn());
  }

  private MockHttpServletRequestBuilder loginRequest() throws Exception {
    return post(UrlConstants.AUTH_BASE + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", EMAIL, "password", PASSWORD)));
  }

  private ResultActions refresh(Session session) throws Exception {
    return mockMvc.perform(
        post(UrlConstants.AUTH_BASE + "/refresh").cookie(session.refreshCookie()));
  }

  private ResultActions me(Session session) throws Exception {
    return mockMvc.perform(
        get(UrlConstants.USER_BASE + "/me")
            .header("Authorization", "Bearer " + session.accessToken()));
  }

  private Session session(MvcResult result) throws Exception {
    MockHttpServletResponse response = result.getResponse();
    Cookie refreshCookie =
        Objects.requireNonNull(response.getCookie(CookieManager.REFRESH_TOKEN_NAME));
    return new Session(
        objectMapper.readTree(response.getContentAsString()).get("accessToken").asText(),
        new Cookie(CookieManager.REFRESH_TOKEN_NAME, refreshCookie.getValue()));
  }

  private record Session(String accessToken, Cookie refreshCookie) {}
}
