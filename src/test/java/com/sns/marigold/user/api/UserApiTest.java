package com.sns.marigold.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sns.marigold.auth.common.csrf.CsrfTokenService;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.tsid.TsidCodec;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.support.ApiIntegrationTest;
import com.sns.marigold.user.entity.User;

public class UserApiTest extends ApiIntegrationTest {

  private static final String CSRF_TOKEN = "test-csrf-token";

  @Test
  @DisplayName("인증 없이 프로필을 조회할 수 있다")
  void getProfile_Success() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    String tsid = TsidCodec.encode(user.getId());

    mockMvc
        .perform(get(UrlConstants.USER_BASE + "/profile/{userId}", tsid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value(user.getNickname()));
  }

  @Test
  @DisplayName("잘못된 ID 형식으로 프로필 조회 시 400이 반환된다")
  void getProfile_InvalidTsid() throws Exception {
    mockMvc
        .perform(get(UrlConstants.USER_BASE + "/profile/invalid_tsid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("인증 없이 사용자 검색이 가능하다")
  void searchUsers_Success() throws Exception {
    mockMvc
        .perform(get(UrlConstants.USER_BASE + "/search").param("query", "tester1")) // 조회할 닉네임
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nickname").value("tester1"));
  }

  @Test
  @DisplayName("인증된 사용자는 보안 설정 상태를 조회할 수 있다")
  void getSecurityInfo_Success() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    user.addEmailAndPassword("tester1@example.com", "encoded-password");
    user.linkOAuth2(ProviderInfo.KAKAO, "kakao-123");
    userRepository.save(user);

    mockMvc
        .perform(
            get(UrlConstants.USER_BASE + "/security")
                .header("Authorization", "Bearer " + getAccessToken(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("tester1@example.com"))
        .andExpect(jsonPath("$.hasLocalCredentials").value(true))
        .andExpect(jsonPath("$.hasOAuth2Link").value(true))
        .andExpect(jsonPath("$.providerInfo").value("KAKAO"));
  }

  @Test
  @DisplayName("OAuth2 가입 사용자는 이메일/비밀번호 로그인 정보를 등록할 수 있다")
  void registerCredentials_Success() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    String requestBody =
        """
        {
          "email": "tester1@example.com",
          "password": "password123!"
        }
        """;

    mockMvc
        .perform(
            post(UrlConstants.USER_BASE + "/credentials")
                .header("Authorization", "Bearer " + getAccessToken(user))
                .header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, CSRF_TOKEN)
                .cookie(getRecentAuthCookie(user), getCsrfCookie(CSRF_TOKEN))
                .contentType("application/json")
                .content(requestBody))
        .andExpect(status().isNoContent());

    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    assertThat(updatedUser.getEmail()).isEqualTo("tester1@example.com");
  }

  @Test
  @DisplayName("Recent auth 없이 이메일 비밀번호 로그인 정보를 등록할 수 없다")
  void registerCredentials_RecentAuthRequired() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    String requestBody =
        """
        {
          "email": "tester1@example.com",
          "password": "password123!"
        }
        """;

    mockMvc
        .perform(
            post(UrlConstants.USER_BASE + "/credentials")
                .header("Authorization", "Bearer " + getAccessToken(user))
                .contentType("application/json")
                .content(requestBody))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("AUTH_RECENT_AUTH_REQUIRED"));
  }

  @Test
  @DisplayName("Recent auth는 실패한 제출 후에도 TTL 동안 재사용할 수 있다")
  void registerCredentials_RecentAuthReusableAfterFailedSubmission() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    User otherUser = java.util.Objects.requireNonNull(tester2);
    otherUser.addEmailAndPassword("used@example.com", "encoded-password");
    userRepository.save(otherUser);

    var recentAuthCookie = getRecentAuthCookie(user);
    String duplicateRequestBody =
        """
        {
          "email": "used@example.com",
          "password": "password123!"
        }
        """;
    String validRequestBody =
        """
        {
          "email": "tester1@example.com",
          "password": "password123!"
        }
        """;

    mockMvc
        .perform(
            post(UrlConstants.USER_BASE + "/credentials")
                .header("Authorization", "Bearer " + getAccessToken(user))
                .header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, CSRF_TOKEN)
                .cookie(recentAuthCookie, getCsrfCookie(CSRF_TOKEN))
                .contentType("application/json")
                .content(duplicateRequestBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"));

    mockMvc
        .perform(
            post(UrlConstants.USER_BASE + "/credentials")
                .header("Authorization", "Bearer " + getAccessToken(user))
                .header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, CSRF_TOKEN)
                .cookie(recentAuthCookie, getCsrfCookie(CSRF_TOKEN))
                .contentType("application/json")
                .content(validRequestBody))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("인증된 사용자는 본인 계정을 삭제할 수 있다")
  void deleteUser_Success() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    mockMvc
        .perform(
            delete(UrlConstants.USER_BASE + "/delete")
                .header("Authorization", "Bearer " + getAccessToken(user))
                .header(CsrfTokenService.CSRF_TOKEN_HEADER_NAME, CSRF_TOKEN)
                .cookie(getRecentAuthCookie(user), getCsrfCookie(CSRF_TOKEN)))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("Recent auth 없이 본인 계정을 삭제할 수 없다")
  void deleteUser_RecentAuthRequired() throws Exception {
    User user = java.util.Objects.requireNonNull(tester1);
    mockMvc
        .perform(
            delete(UrlConstants.USER_BASE + "/delete")
                .header("Authorization", "Bearer " + getAccessToken(user)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("AUTH_RECENT_AUTH_REQUIRED"));
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 계정 삭제를 할 수 없다 (401)")
  void deleteUser_Unauthorized() throws Exception {
    mockMvc
        .perform(delete(UrlConstants.USER_BASE + "/delete"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.errorCode").value("AUTH_UNAUTHORIZED"));
  }
}
