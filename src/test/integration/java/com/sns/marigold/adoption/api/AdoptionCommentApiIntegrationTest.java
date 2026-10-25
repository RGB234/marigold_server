package com.sns.marigold.adoption.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;

import com.sns.marigold.adoption.entity.AdoptionComment;
import com.sns.marigold.adoption.entity.AdoptionCommentImage;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.repository.AdoptionCommentImageRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.support.ApiIntegrationTest;
import com.sns.marigold.user.entity.User;

// OSIV를 끈 상태에서 트랜잭션 밖에서 읽은 게시글·부모 댓글을 참조해 저장하는 경로를 확인한다
public class AdoptionCommentApiIntegrationTest extends ApiIntegrationTest {

  @Autowired private AdoptionPostRepository adoptionPostRepository;

  @Autowired private AdoptionCommentRepository adoptionCommentRepository;

  @Autowired private AdoptionCommentImageRepository adoptionCommentImageRepository;

  private Long postId;

  @BeforeEach
  void setUp() {
    AdoptionPost post =
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
    postId = post.getId();
    givenUploadedJpeg();
  }

  @Test
  @DisplayName("게시글 작성자는 이미지와 함께 댓글을 작성한다")
  void createComment_WithImage() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    String uploadToken = issueJpegUploadToken(writer, UploadPurpose.ADOPTION_COMMENT_IMAGE);

    createComment(writer, Map.of("content", "댓글입니다", "uploadTokens", List.of(uploadToken)));

    assertEquals(1, adoptionCommentRepository.count());
    assertEquals(1, adoptionCommentImageRepository.count());
  }

  @Test
  @DisplayName("부모 댓글을 지정해 대댓글을 작성한다")
  void createComment_Reply() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    String parentId = createComment(writer, Map.of("content", "부모 댓글"));

    String replyId = createComment(writer, Map.of("content", "대댓글", "parentId", parentId));

    AdoptionComment reply = adoptionCommentRepository.findById(Long.valueOf(replyId)).orElseThrow();
    assertEquals(Long.valueOf(parentId), reply.getParent().getId());
  }

  @Test
  @DisplayName("게시글 작성자나 입양자가 아니면 댓글을 작성할 수 없다")
  void createComment_AccessDenied() throws Exception {
    mockMvc
        .perform(
            post(UrlConstants.ADOPTION_BASE + "/{postId}/comments", postId)
                .header(
                    "Authorization", "Bearer " + getAccessToken(Objects.requireNonNull(tester2)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("content", "댓글입니다"))))
        .andExpect(status().isForbidden());

    assertEquals(0, adoptionCommentRepository.count());
  }

  @Test
  @DisplayName("댓글을 수정하며 이미지를 새 이미지로 교체한다")
  void updateComment_ReplaceImage() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    String commentId =
        createComment(
            writer,
            Map.of(
                "content",
                "댓글입니다",
                "uploadTokens",
                List.of(issueJpegUploadToken(writer, UploadPurpose.ADOPTION_COMMENT_IMAGE))));
    String oldFile = storedCommentImages().get(0);

    updateComment(
        writer,
        commentId,
        Map.of(
            "content",
            "수정한 댓글",
            "uploadTokens",
            List.of(issueJpegUploadToken(writer, UploadPurpose.ADOPTION_COMMENT_IMAGE))));

    List<String> images = storedCommentImages();
    assertEquals(1, images.size());
    assertNotEquals(oldFile, images.get(0));
  }

  @Test
  @DisplayName("댓글을 수정하며 이미지를 삭제한다")
  void updateComment_RemoveImage() throws Exception {
    User writer = Objects.requireNonNull(tester1);
    String commentId =
        createComment(
            writer,
            Map.of(
                "content",
                "댓글입니다",
                "uploadTokens",
                List.of(issueJpegUploadToken(writer, UploadPurpose.ADOPTION_COMMENT_IMAGE))));

    updateComment(writer, commentId, Map.of("content", "수정한 댓글", "removeImage", true));

    assertEquals(0, adoptionCommentImageRepository.count());
  }

  private String createComment(@NonNull User user, Map<String, Object> body) throws Exception {
    String response =
        mockMvc
            .perform(
                post(UrlConstants.ADOPTION_BASE + "/{postId}/comments", postId)
                    .header("Authorization", "Bearer " + getAccessToken(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }

  private void updateComment(@NonNull User user, String commentId, Map<String, Object> body)
      throws Exception {
    mockMvc
        .perform(
            patch(UrlConstants.ADOPTION_BASE + "/{postId}/comments/{commentId}", postId, commentId)
                .header("Authorization", "Bearer " + getAccessToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)))
        .andExpect(status().isNoContent());
  }

  private List<String> storedCommentImages() {
    return adoptionCommentImageRepository.findAll().stream()
        .map(AdoptionCommentImage::getStoredFileName)
        .toList();
  }
}
