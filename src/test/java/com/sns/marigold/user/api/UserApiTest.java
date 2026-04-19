package com.sns.marigold.user.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.support.ApiIntegrationTest;
import com.sns.marigold.user.exception.UserException;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class UserApiTest extends ApiIntegrationTest {

  @Test
  @DisplayName("인증 없이 프로필을 조회할 수 있다")
  void getProfile_Success() throws Exception {
    String tsid = TSID.from(tester1.getId()).toString();

    mockMvc
        .perform(get(UrlConstants.USER_BASE + "/profile/{userId}", tsid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.nickname").value(tester1.getNickname()));
  }

  @Test
  @DisplayName("잘못된 ID 형식으로 프로필 조회 시 404가 반환된다")
  void getProfile_InvalidTsid() throws Exception {
    mockMvc
        .perform(get(UrlConstants.USER_BASE + "/profile/invalid_tsid"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value(UserException.forUserNotFound().getMessage()));
  }

  @Test
  @DisplayName("인증 없이 사용자 검색이 가능하다")
  void searchUsers_Success() throws Exception {
    mockMvc
        .perform(get(UrlConstants.USER_BASE + "/search").param("query", "tester1")) // 조회할 닉네임
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data[0].nickname").value("tester1"));
  }

  @Test
  @DisplayName("인증된 사용자는 본인 계정을 삭제할 수 있다")
  void deleteUser_Success() throws Exception {
    mockMvc
        .perform(
            delete(UrlConstants.USER_BASE + "/delete")
                .header("Authorization", "Bearer " + getAccessToken(tester1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 계정 삭제를 할 수 없다 (401)")
  void deleteUser_Unauthorized() throws Exception {
    mockMvc
        .perform(delete(UrlConstants.USER_BASE + "/delete"))
        .andExpect(status().isUnauthorized());
  }
}
