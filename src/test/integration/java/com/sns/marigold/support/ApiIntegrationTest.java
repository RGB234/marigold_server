package com.sns.marigold.support;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.common.session.AuthSessionStore;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.objectstore.StoredFileHead;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;

@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends BaseIntegrationTest {

  // Tika가 image/jpeg로 감지하는 최소 바이트
  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00
  };

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected JwtManager jwtManager;

  @Autowired protected AuthSessionService authSessionService;

  @Autowired protected AuthSessionStore authSessionStore;

  @Autowired protected UserRepository userRepository;

  protected User tester1;
  protected User tester2;

  @BeforeEach
  void apiSetUp() {
    User user1 =
        Objects.requireNonNull(User.builder().nickname("tester1").role(Role.ROLE_PERSON).build());
    User user2 =
        Objects.requireNonNull(User.builder().nickname("tester2").role(Role.ROLE_PERSON).build());
    tester1 = userRepository.save(user1);

    tester2 = userRepository.save(user2);
  }

  /** 방금 로그인한 세션의 access token. 요청마다 Redis 세션을 확인하고, 최근 인증도 통과한다. */
  protected String getAccessToken(@NonNull User tester) {
    return authSessionService.start(principal(tester)).accessToken();
  }

  /** 로그인한 지 한 시간 지난 세션의 access token. 최근 인증이 필요한 API의 거부를 확인할 때 쓴다. */
  protected String getStaleAccessToken(@NonNull User tester) {
    String sessionId = UUID.randomUUID().toString();
    Instant now = Instant.now();
    authSessionStore.create(
        sessionId,
        tester.getId(),
        UUID.randomUUID().toString(),
        now.minus(Duration.ofHours(1)),
        now.plus(Duration.ofDays(1)),
        now.plus(Duration.ofDays(14)));
    return jwtManager.createAccessToken(principal(tester), sessionId);
  }

  private CustomPrincipal principal(User tester) {
    return new CustomPrincipal(
        tester.getId(),
        Collections.singletonList(new SimpleGrantedAuthority(tester.getRole().name())),
        Map.of(),
        AuthStatus.LOGIN_SUCCESS);
  }

  /** 클라이언트가 PUT을 끝낸 상태: 확정 단계가 읽는 파일 앞부분을 JPEG로 돌려준다. */
  protected void givenUploadedJpeg() {
    given(objectStorage.readFileHead(anyString(), anyInt()))
        .willReturn(new StoredFileHead(JPEG_BYTES.length, JPEG_BYTES, "etag"));
  }

  /** 업로드 URL 발급 API로 JPEG 한 장의 업로드 토큰을 받는다. */
  protected String issueJpegUploadToken(@NonNull User user, UploadPurpose purpose)
      throws Exception {
    Map<String, Object> request =
        Map.of(
            "purpose",
            purpose.name(),
            "files",
            List.of(Map.of("fileName", "test1.jpg", "size", JPEG_BYTES.length)));
    String response =
        mockMvc
            .perform(
                post(UrlConstants.STORAGE_BASE + "/uploads")
                    .header("Authorization", "Bearer " + getAccessToken(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get(0).get("uploadToken").asText();
  }

  protected String json(Object body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }
}
