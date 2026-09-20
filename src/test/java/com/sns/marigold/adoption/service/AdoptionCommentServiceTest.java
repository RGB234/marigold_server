package com.sns.marigold.adoption.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.adoption.dto.AdoptionCommentCreateDto;
import com.sns.marigold.adoption.dto.AdoptionCommentUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionComment;
import com.sns.marigold.adoption.entity.AdoptionCommentImage;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.exception.AdoptionCommentException;
import com.sns.marigold.adoption.repository.AdoptionAdopterRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentImageRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.event.DeleteUploadedStorageFilesEvent;
import com.sns.marigold.storage.service.StorageDirectory;
import com.sns.marigold.storage.service.StorageService;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdoptionCommentServiceTest {

  @Mock private AdoptionCommentRepository adoptionCommentRepository;

  @Mock private AdoptionPostRepository adoptionPostRepository;

  @Mock private AdoptionAdopterRepository adoptionAdopterRepository;

  @Mock private UserService userService;

  @Mock private StorageService storageService;

  @Mock private TransactionTemplate transactionTemplate;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private AdoptionCommentImageRepository adoptionCommentImageRepository;

  private AdoptionCommentService adoptionCommentService;
  private User writer;
  private AdoptionPost post;
  private AdoptionComment comment;

  @BeforeEach
  void setUp() {
    adoptionCommentService =
        new AdoptionCommentService(
            adoptionCommentRepository,
            adoptionPostRepository,
            adoptionAdopterRepository,
            userService,
            storageService,
            transactionTemplate,
            eventPublisher,
            adoptionCommentImageRepository);

    writer = User.builder().id(1L).nickname("writer").role(Role.ROLE_PERSON).build();
    post = AdoptionPost.builder().writer(writer).title("post").build();
    ReflectionTestUtils.setField(post, "id", 100L);
    comment =
        AdoptionComment.builder().adoptionPost(post).writer(writer).content("comment").build();
    comment.addImage(
        AdoptionCommentImage.builder()
            .storedFileName("adoption/comment/11111111-1111-1111-1111-111111111111.jpg")
            .originalFileName("comment-image-1.jpg")
            .build());
    comment.addImage(
        AdoptionCommentImage.builder()
            .storedFileName("adoption/comment/22222222-2222-2222-2222-222222222222.jpg")
            .originalFileName("comment-image-2.jpg")
            .build());

    org.mockito.Mockito.lenient()
        .doAnswer(
            invocation -> {
              TransactionCallback<?> action = invocation.getArgument(0);
              return action.doInTransaction(org.mockito.Mockito.mock(TransactionStatus.class));
            })
        .when(transactionTemplate)
        .execute(any());

    org.mockito.Mockito.lenient()
        .doAnswer(
            invocation -> {
              Consumer<TransactionStatus> action = invocation.getArgument(0);
              action.accept(org.mockito.Mockito.mock(TransactionStatus.class));
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
  }

  @Test
  @DisplayName("댓글 생성 시 DB 저장 전에 새 이미지의 롤백 삭제 이벤트를 발행한다.")
  void createComment_PublishesRollbackEventBeforeSave() {
    MockMultipartFile image =
        new MockMultipartFile("images", "new.jpg", "image/jpeg", new byte[] {1});
    AdoptionCommentCreateDto dto =
        AdoptionCommentCreateDto.builder().content("new comment").images(List.of(image)).build();
    String storedFileName = "adoption/comment/33333333-3333-3333-3333-333333333333.jpg";
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(post));
    given(userService.findEntityById(1L)).willReturn(writer);
    given(storageService.uploadImages(any(), eq(StorageDirectory.ADOPTION_COMMENT)))
        .willReturn(List.of(new ImageUploadDto(storedFileName, "new.jpg")));
    given(adoptionCommentRepository.save(any(AdoptionComment.class)))
        .willAnswer(
            invocation -> {
              AdoptionComment saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", 10L);
              return saved;
            });

    assertThat(adoptionCommentService.createComment(100L, 1L, dto)).isEqualTo(10L);

    var order = inOrder(eventPublisher, adoptionCommentRepository);
    order
        .verify(eventPublisher)
        .publishEvent(new DeleteUploadedStorageFilesEvent(List.of(storedFileName)));
    order.verify(adoptionCommentRepository).save(any(AdoptionComment.class));
    verify(storageService, never()).deleteUploadedImages(any());
  }

  @Test
  @DisplayName("댓글 작성자는 댓글 내용을 수정할 수 있다.")
  void updateComment_Success() {
    // given
    AdoptionCommentUpdateDto dto =
        AdoptionCommentUpdateDto.builder().content("updated comment").build();
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when
    adoptionCommentService.updateComment(100L, 10L, 1L, dto);

    // then
    assertThat(comment.getContent()).isEqualTo("updated comment");
    assertThat(comment.getImages()).hasSize(2);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("댓글 수정 시 새 이미지가 있으면 기존 이미지를 교체하고 기존 이미지 삭제 이벤트를 발행한다.")
  void updateComment_ReplaceImage() {
    // given
    MockMultipartFile newImage =
        new MockMultipartFile("images", "new.jpg", "image/jpeg", "new-image".getBytes());
    AdoptionCommentUpdateDto dto =
        AdoptionCommentUpdateDto.builder()
            .content("updated comment")
            .images(List.of(newImage))
            .build();
    List<ImageUploadDto> uploadedImages =
        List.of(
            ImageUploadDto.builder()
                .storedFileName("adoption/comment/33333333-3333-3333-3333-333333333333.jpg")
                .originalFileName("new.jpg")
                .build());
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));
    given(storageService.uploadImages(any(), eq(StorageDirectory.ADOPTION_COMMENT)))
        .willReturn(uploadedImages);

    // when
    adoptionCommentService.updateComment(100L, 10L, 1L, dto);

    // then
    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    assertThat(comment.getContent()).isEqualTo("updated comment");
    assertThat(comment.getImages()).hasSize(1);
    assertThat(comment.getImages().get(0).getStoredFileName())
        .isEqualTo("adoption/comment/33333333-3333-3333-3333-333333333333.jpg");
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames())
        .containsExactlyInAnyOrder(
            "adoption/comment/11111111-1111-1111-1111-111111111111.jpg",
            "adoption/comment/22222222-2222-2222-2222-222222222222.jpg");
    verify(eventPublisher)
        .publishEvent(
            new DeleteUploadedStorageFilesEvent(
                List.of("adoption/comment/33333333-3333-3333-3333-333333333333.jpg")));
  }

  @Test
  @DisplayName("댓글 수정 시 이미지 삭제를 요청하면 기존 이미지를 제거하고 삭제 이벤트를 발행한다.")
  void updateComment_RemoveImage() {
    // given
    AdoptionCommentUpdateDto dto =
        AdoptionCommentUpdateDto.builder().content("updated comment").removeImage(true).build();
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when
    adoptionCommentService.updateComment(100L, 10L, 1L, dto);

    // then
    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    assertThat(comment.getContent()).isEqualTo("updated comment");
    assertThat(comment.getImages()).isEmpty();
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames())
        .containsExactlyInAnyOrder(
            "adoption/comment/11111111-1111-1111-1111-111111111111.jpg",
            "adoption/comment/22222222-2222-2222-2222-222222222222.jpg");
  }

  @Test
  @DisplayName("삭제된 댓글은 수정할 수 없다.")
  void updateComment_AlreadyDeleted() {
    // given
    AdoptionCommentUpdateDto dto =
        AdoptionCommentUpdateDto.builder().content("updated comment").build();
    comment.softDelete();
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> adoptionCommentService.updateComment(100L, 10L, 1L, dto))
        .isInstanceOf(AdoptionCommentException.class)
        .hasMessageContaining(AdoptionCommentException.forAdoptionCommentDeleted().getMessage());
    assertThat(comment.getContent()).isEqualTo("comment");
  }

  @Test
  @DisplayName("작성자가 아니라면 댓글을 수정할 수 없다.")
  void updateComment_NotWriter() {
    // given
    AdoptionCommentUpdateDto dto =
        AdoptionCommentUpdateDto.builder().content("updated comment").build();
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> adoptionCommentService.updateComment(100L, 10L, 2L, dto))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forAccessDenied().getMessage());
    assertThat(comment.getContent()).isEqualTo("comment");
  }

  @Test
  @DisplayName("다른 게시글의 댓글은 수정할 수 없다.")
  void updateComment_PostMismatch() {
    // given
    AdoptionCommentUpdateDto dto =
        AdoptionCommentUpdateDto.builder().content("updated comment").build();
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> adoptionCommentService.updateComment(999L, 10L, 1L, dto))
        .isInstanceOf(AdoptionCommentException.class)
        .hasMessageContaining(
            AdoptionCommentException.forAdoptionCommentPostMismatch().getMessage());
    assertThat(comment.getContent()).isEqualTo("comment");
  }

  @Test
  @DisplayName("댓글 삭제 시 댓글을 소프트 삭제하고 이미지 삭제 이벤트를 발행한다.")
  void deleteComment_Success() {
    // given
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when
    adoptionCommentService.deleteComment(10L, 1L);

    // then
    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    assertThat(comment.getDeletedAt()).isNotNull();
    assertThat(comment.getImages()).isEmpty();
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames())
        .containsExactlyInAnyOrder(
            "adoption/comment/11111111-1111-1111-1111-111111111111.jpg",
            "adoption/comment/22222222-2222-2222-2222-222222222222.jpg");
  }

  @Test
  @DisplayName("댓글 이미지 삭제 이벤트는 트랜잭션 안에서 발행한다.")
  void deleteComment_PublishesDeleteEventInsideTransaction() {
    // given
    AtomicBoolean inTransaction = new AtomicBoolean(false);
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    org.mockito.Mockito.doAnswer(
            invocation -> {
              Consumer<TransactionStatus> action = invocation.getArgument(0);
              inTransaction.set(true);
              try {
                action.accept(org.mockito.Mockito.mock(TransactionStatus.class));
              } finally {
                inTransaction.set(false);
              }
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());

    org.mockito.Mockito.doAnswer(
            invocation -> {
              assertThat(inTransaction.get()).isTrue();
              return null;
            })
        .when(eventPublisher)
        .publishEvent(any(DeleteOldStorageFilesEvent.class));

    // when
    adoptionCommentService.deleteComment(10L, 1L);

    // then
    verify(eventPublisher, times(1)).publishEvent(any(DeleteOldStorageFilesEvent.class));
  }

  @Test
  @DisplayName("삭제된 댓글을 다시 삭제할 수 없다.")
  void deleteComment_AlreadyDeleted() {
    // given
    comment.softDelete();
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> adoptionCommentService.deleteComment(10L, 1L))
        .isInstanceOf(AdoptionCommentException.class)
        .hasMessageContaining(AdoptionCommentException.forAdoptionCommentDeleted().getMessage());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("작성자가 아니라면 댓글을 삭제할 수 없다.")
  void deleteComment_NotWriter() {
    // given
    given(adoptionCommentRepository.findById(10L)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> adoptionCommentService.deleteComment(10L, 2L))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forAccessDenied().getMessage());
    assertThat(comment.getDeletedAt()).isNull();
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("게시글 삭제 시 해당 게시글의 댓글, 대댓글 FK, 댓글 이미지를 일괄 삭제한다.")
  void deleteCommentsByPostId_Success() {
    // given
    given(adoptionCommentImageRepository.findStoredFileNamesByAdoptionPostId(100L))
        .willReturn(
            List.of(
                "adoption/comment/11111111-1111-1111-1111-111111111111.jpg",
                "adoption/comment/22222222-2222-2222-2222-222222222222.jpg"));

    // when
    adoptionCommentService.deleteCommentsByPostId(100L);

    // then
    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    verify(adoptionCommentImageRepository, times(1)).findStoredFileNamesByAdoptionPostId(100L);
    verify(adoptionCommentImageRepository, times(1)).deleteByAdoptionPostId(100L);
    verify(adoptionCommentRepository, times(1)).clearParentByAdoptionPostId(100L);
    verify(adoptionCommentRepository, times(1)).deleteByAdoptionPostId(100L);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames())
        .containsExactlyInAnyOrder(
            "adoption/comment/11111111-1111-1111-1111-111111111111.jpg",
            "adoption/comment/22222222-2222-2222-2222-222222222222.jpg");
  }
}
