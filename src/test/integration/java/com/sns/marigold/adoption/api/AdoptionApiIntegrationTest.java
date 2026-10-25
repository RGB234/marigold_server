package com.sns.marigold.adoption.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.repository.AdoptionPostImageRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.service.StoredFileHead;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.support.ApiIntegrationTest;
import com.sns.marigold.user.entity.User;

public class AdoptionApiIntegrationTest extends ApiIntegrationTest {

  // Tika가 image/jpeg로 감지하는 최소 바이트
  private static final byte[] JPEG_BYTES = {
    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00
  };

  @Autowired private AdoptionPostRepository adoptionPostRepository;

  @Autowired private AdoptionPostImageRepository adoptionPostImageRepository;

  @Nullable private AdoptionPost defaultPost;

  @BeforeEach
  void setUp() {
    defaultPost =
        adoptionPostRepository.save(
            Objects.requireNonNull(
                AdoptionPost.builder()
                    .writer(tester1)
                    .title("Original Title")
                    .species(Species.DOG)
                    .sex(Sex.MALE)
                    .age(1)
                    .weight(2.0)
                    .area("Seoul")
                    .neutering(Neutering.NO)
                    .features("12345678901234567890")
                    .build()));

    // 클라이언트가 PUT을 끝낸 상태: 확정 단계가 읽는 파일 앞부분을 JPEG로 돌려준다
    given(storageService.readFileHead(anyString(), anyInt()))
        .willReturn(new StoredFileHead(JPEG_BYTES.length, JPEG_BYTES));
  }

  @Test
  @DisplayName("발급받은 업로드 토큰으로 입양 게시글을 생성한다")
  void createAdoptionPost() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    String uploadToken = issueImageUploadToken(writer);

    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE)
                .header("Authorization", "Bearer " + getAccessToken(writer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(postBody("Test Title", "12345678901234567890", uploadToken))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString());

    // DB 상태 검증: setUp()에서 1개 + 새로 생성 1개 = 총 2개, 이미지는 새 게시글의 1장
    assertEquals(2, adoptionPostRepository.count());
    assertEquals(1, adoptionPostImageRepository.count());
  }

  @Test
  @DisplayName("다른 사용자에게 발급된 업로드 토큰으로는 게시글을 생성할 수 없다")
  void createAdoptionPost_OtherUsersUploadToken() throws Exception {
    String othersUploadToken = issueImageUploadToken(Objects.requireNonNull(tester2));

    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE)
                .header(
                    "Authorization", "Bearer " + getAccessToken(Objects.requireNonNull(tester1)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(postBody("Test Title", "12345678901234567890", othersUploadToken))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("UPLOAD_TOKEN_INVALID"));

    assertEquals(1, adoptionPostRepository.count());
  }

  @Test
  @DisplayName("이미 게시글에 쓴 업로드 토큰으로 다른 게시글을 만들면 409를 반환한다")
  void createAdoptionPost_ReusedUploadToken() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    String uploadToken = issueImageUploadToken(writer);
    String body = json(postBody("Test Title", "12345678901234567890", uploadToken));

    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE)
                .header("Authorization", "Bearer " + getAccessToken(writer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE)
                .header("Authorization", "Bearer " + getAccessToken(writer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("UPLOAD_ALREADY_USED"));

    assertEquals(2, adoptionPostRepository.count());
    assertEquals(1, adoptionPostImageRepository.count());
  }

  @Test
  @DisplayName("입양 게시글 생성 시 입력값이 유효하지 않으면 400에러를 반환한다")
  void createAdoptionPost_BadRequest() throws Exception {
    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE)
                .header(
                    "Authorization", "Bearer " + getAccessToken(Objects.requireNonNull(tester1)))
                .contentType(MediaType.APPLICATION_JSON)
                // 빈 제목, 20자 미만 특징 (에러 유발)
                .content(json(postBody("", "짧은 내용", "unused-token"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 입양 게시글을 생성할 수 없다. 401에러를 반환한다.")
  void createAdoptionPost_Unauthorized() throws Exception {
    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(postBody("Test Title", "12345678901234567890", "unused-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value(AuthException.forUnauthorized().getMessage()));
  }

  @Test
  @DisplayName("본인의 입양 게시글을 수정한다")
  void updateAdoptionPost() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    Long postId = Objects.requireNonNull(Objects.requireNonNull(defaultPost).getId());
    String uploadToken = issueImageUploadToken(writer);

    mockMvc
        .perform(
            patch(UrlConstants.ADOPTION_BASE + "/{id}", postId)
                .header("Authorization", "Bearer " + getAccessToken(writer))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(updateBody(uploadToken))))
        .andExpect(status().isNoContent());

    // DB 상태 검증
    AdoptionPost updated = adoptionPostRepository.findById(postId).orElseThrow();
    assertEquals("Updated Title", updated.getTitle());
    assertEquals(Species.CAT, updated.getSpecies());
    assertEquals(1, adoptionPostImageRepository.count());
  }

  @Test
  @DisplayName("로그인 하지 않은 사용자는 입양 게시글을 수정할 수 없다. 401에러를 반환한다.")
  void updateAdoptionPost_Unauthorized() throws Exception {
    AdoptionPost post = Objects.requireNonNull(defaultPost);
    mockMvc
        .perform(
            patch(UrlConstants.ADOPTION_BASE + "/{id}", post.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(updateBody("unused-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.detail").value(AuthException.forUnauthorized().getMessage()));
  }

  @Test
  @DisplayName("본인이 작성하지 않은 입양 게시글을 수정할 수 없다. 403에러를 반환한다.")
  void updateAdoptionPost_AccessDenied() throws Exception {
    AdoptionPost post = Objects.requireNonNull(defaultPost);
    mockMvc
        .perform(
            patch(UrlConstants.ADOPTION_BASE + "/{id}", post.getId())
                .header(
                    "Authorization", "Bearer " + getAccessToken(Objects.requireNonNull(tester2)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(updateBody("unused-token"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.detail").value(AuthException.forAccessDenied().getMessage()));
  }

  @Test
  @DisplayName("본인의 입양 게시글을 삭제한다")
  void deleteAdoptionPost() throws Exception {
    AdoptionPost post = Objects.requireNonNull(defaultPost);
    Long postId = Objects.requireNonNull(post.getId());
    mockMvc
        .perform(
            delete(UrlConstants.ADOPTION_BASE + "/{id}", postId)
                .header(
                    "Authorization", "Bearer " + getAccessToken(Objects.requireNonNull(tester1))))
        .andExpect(status().isNoContent());

    // DB 상태 검증
    AdoptionPost deletedPost = adoptionPostRepository.findById(postId).orElseThrow();
    assertNotNull(deletedPost.getDeletedAt());
  }

  @Test
  @DisplayName("로그인 하지 않은 사용자는 입양 게시글을 삭제할 수 없다. 401에러를 반환한다.")
  void deleteAdoptionPost_Unauthorized() throws Exception {
    AdoptionPost post = Objects.requireNonNull(defaultPost);
    mockMvc
        .perform(delete(UrlConstants.ADOPTION_BASE + "/{id}", post.getId()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @DisplayName("본인이 작성하지 않은 입양 게시글을 삭제할 수 없다. 403에러를 반환한다.")
  void deleteAdoptionPost_AccessDenied() throws Exception {
    AdoptionPost post = Objects.requireNonNull(defaultPost);
    mockMvc
        .perform(
            delete(UrlConstants.ADOPTION_BASE + "/{id}", post.getId())
                .header(
                    "Authorization", "Bearer " + getAccessToken(Objects.requireNonNull(tester2))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @DisplayName("인증되지 않은 익명 사용자도 입양 게시글 목록을 조회할 수 있다")
  void searchAdoptionPosts() throws Exception {
    mockMvc
        .perform(get(UrlConstants.ADOPTION_BASE).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].title").value("Original Title"));
  }

  private String issueImageUploadToken(@NonNull User user) throws Exception {
    Map<String, Object> request =
        Map.of(
            "purpose",
            UploadPurpose.ADOPTION_POST_IMAGE.name(),
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

  private Map<String, Object> postBody(String title, String features, String uploadToken) {
    Map<String, Object> body = new HashMap<>();
    body.put("species", Species.DOG.name());
    body.put("title", title);
    body.put("age", 2);
    body.put("sex", Sex.MALE.name());
    body.put("area", "Seoul");
    body.put("weight", 5.0);
    body.put("neutering", Neutering.YES.name());
    body.put("features", features);
    body.put("uploadTokens", List.of(uploadToken));
    return body;
  }

  private Map<String, Object> updateBody(String uploadToken) {
    Map<String, Object> body = postBody("Updated Title", "Updated 123456789012", uploadToken);
    body.put("species", Species.CAT.name());
    body.put("sex", Sex.FEMALE.name());
    body.put("area", "Busan");
    return body;
  }

  private String json(Object body) throws Exception {
    return objectMapper.writeValueAsString(body);
  }
}
