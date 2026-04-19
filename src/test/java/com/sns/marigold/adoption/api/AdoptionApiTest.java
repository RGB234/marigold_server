package com.sns.marigold.adoption.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.support.ApiIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

public class AdoptionApiTest extends ApiIntegrationTest {

  @Autowired private AdoptionPostRepository adoptionPostRepository;

  private AdoptionPost defaultPost;
  private MockMultipartFile defaultImage;

  @BeforeEach
  void setUp() {
    defaultPost =
        adoptionPostRepository.save(
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
                .build());

    defaultImage = new MockMultipartFile("images", "test1.jpg", "image/jpeg", "dummy".getBytes());
  }

  @Test
  @DisplayName("입양 게시글을 정상적으로 생성한다")
  void createAdoptionPost() throws Exception {
    given(s3Service.uploadImagesToS3(any()))
        .willReturn(
            List.of(
                ImageUploadDto.builder()
                    .storedFileName("stored1.jpg")
                    .originalFileName("test1.jpg")
                    .build()));

    mockMvc
        .perform(
            multipart(UrlConstants.ADOPTION_BASE)
                .file(defaultImage)
                .param("species", Species.DOG.name())
                .param("title", "Test Title")
                .param("age", "2")
                .param("sex", Sex.MALE.name())
                .param("area", "Seoul")
                .param("weight", "5.0")
                .param("neutering", Neutering.YES.name())
                .param("features", "12345678901234567890") // 20자 이상
                .header("Authorization", "Bearer " + getAccessToken(tester1))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.id").isNumber());

    // DB 상태 검증: setUp()에서 1개 + 새로 생성 1개 = 총 2개
    assertEquals(2, adoptionPostRepository.count());
  }

  @Test
  @DisplayName("입양 게시글 생성 시 입력값이 유효하지 않으면 400에러를 반환한다")
  void createAdoptionPost_BadRequest() throws Exception {
    mockMvc
        .perform(
            multipart(UrlConstants.ADOPTION_BASE)
                .file(defaultImage)
                .param("species", Species.DOG.name())
                .param("title", "") // 빈 제목 (에러 유발)
                .param("age", "2")
                .param("sex", Sex.MALE.name())
                .param("area", "Seoul")
                .param("weight", "5.0")
                .param("neutering", Neutering.YES.name())
                .param("features", "짧은 내용") // 20자 미만 (에러 유발)
                .header("Authorization", "Bearer " + getAccessToken(tester1))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT_VALUE"));
  }

  @Test
  @DisplayName("인증되지 않은 사용자는 입양 게시글을 생성할 수 없다. 401에러를 반환한다.")
  void createAdoptionPost_Unauthorized() throws Exception {
    given(s3Service.uploadImagesToS3(any()))
        .willReturn(
            List.of(
                ImageUploadDto.builder()
                    .storedFileName("stored1.jpg")
                    .originalFileName("test1.jpg")
                    .build()));

    mockMvc
        .perform(
            multipart(UrlConstants.ADOPTION_BASE)
                .file(defaultImage)
                .param("species", Species.DOG.name())
                .param("title", "Test Title")
                .param("age", "2")
                .param("sex", Sex.MALE.name())
                .param("area", "Seoul")
                .param("weight", "5.0")
                .param("neutering", Neutering.YES.name())
                .param("features", "12345678901234567890") // 20자 이상
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value(AuthException.forUnauthorized().getMessage()));
  }

  @Test
  @DisplayName("본인의 입양 게시글을 수정한다")
  void updateAdoptionPost() throws Exception {
    given(s3Service.uploadImagesToS3(any()))
        .willReturn(
            List.of(
                ImageUploadDto.builder()
                    .storedFileName("stored_update.jpg")
                    .originalFileName("update.jpg")
                    .build()));

    mockMvc
        .perform(
            multipart(UrlConstants.ADOPTION_BASE + "/{id}", defaultPost.getId())
                .file(defaultImage)
                .param("species", Species.CAT.name())
                .param("title", "Updated Title")
                .param("age", "3")
                .param("sex", Sex.FEMALE.name())
                .param("area", "Busan")
                .param("weight", "4.0")
                .param("neutering", Neutering.YES.name())
                .param("features", "Updated 123456789012")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PATCH.name());
                      return request;
                    })
                .header("Authorization", "Bearer " + getAccessToken(tester1))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    // DB 상태 검증
    AdoptionPost updated = adoptionPostRepository.findById(defaultPost.getId()).orElseThrow();
    assertEquals("Updated Title", updated.getTitle());
    assertEquals(Species.CAT, updated.getSpecies());
  }

  @Test
  @DisplayName("로그인 하지 않은 사용자는 입양 게시글을 수정할 수 없다. 401에러를 반환한다.")
  void updateAdoptionPost_Unauthorized() throws Exception {
    mockMvc
        .perform(
            multipart(UrlConstants.ADOPTION_BASE + "/{id}", defaultPost.getId())
                .file(defaultImage)
                .param("species", Species.CAT.name())
                .param("title", "Updated Title")
                .param("age", "3")
                .param("sex", Sex.FEMALE.name())
                .param("area", "Busan")
                .param("weight", "4.0")
                .param("neutering", Neutering.YES.name())
                .param("features", "Updated 123456789012")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PATCH.name());
                      return request;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value(AuthException.forUnauthorized().getMessage()));
  }

  @Test
  @DisplayName("본인이 작성하지 않은 입양 게시글을 수정할 수 없다. 403에러를 반환한다.")
  void updateAdoptionPost_AccessDenied() throws Exception {
    mockMvc
        .perform(
            multipart(UrlConstants.ADOPTION_BASE + "/{id}", defaultPost.getId())
                .file(defaultImage)
                .param("species", Species.CAT.name())
                .param("title", "Updated Title")
                .param("age", "3")
                .param("sex", Sex.FEMALE.name())
                .param("area", "Busan")
                .param("weight", "4.0")
                .param("neutering", Neutering.YES.name())
                .param("features", "Updated 123456789012")
                .with(
                    request -> {
                      request.setMethod(HttpMethod.PATCH.name());
                      return request;
                    })
                .header("Authorization", "Bearer " + getAccessToken(tester2))
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value(AuthException.forAccessDenied().getMessage()));
  }

  @Test
  @DisplayName("본인의 입양 게시글을 삭제한다")
  void deleteAdoptionPost() throws Exception {
    mockMvc
        .perform(
            delete(UrlConstants.ADOPTION_BASE + "/{id}", defaultPost.getId())
                .header("Authorization", "Bearer " + getAccessToken(tester1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    // DB 상태 검증
    AdoptionPost deletedPost = adoptionPostRepository.findById(defaultPost.getId()).orElseThrow();
    assertTrue(deletedPost.getDeletedAt() != null);
  }

  @Test
  @DisplayName("로그인 하지 않은 사용자는 입양 게시글을 삭제할 수 없다. 401에러를 반환한다.")
  void deleteAdoptionPost_Unauthorized() throws Exception {
    mockMvc
        .perform(delete(UrlConstants.ADOPTION_BASE + "/{id}", defaultPost.getId()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @DisplayName("본인이 작성하지 않은 입양 게시글을 삭제할 수 없다. 403에러를 반환한다.")
  void deleteAdoptionPost_AccessDenied() throws Exception {
    mockMvc
        .perform(
            delete(UrlConstants.ADOPTION_BASE + "/{id}", defaultPost.getId())
                .header("Authorization", "Bearer " + getAccessToken(tester2)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @DisplayName("인증되지 않은 익명 사용자도 입양 게시글 목록을 조회할 수 있다")
  void searchAdoptionPosts() throws Exception {
    mockMvc
        .perform(get(UrlConstants.ADOPTION_BASE).param("page", "0").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.content").isArray())
        .andExpect(jsonPath("$.data.content[0].title").value("Original Title"));
  }
}
