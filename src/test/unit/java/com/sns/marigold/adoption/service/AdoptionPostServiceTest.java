package com.sns.marigold.adoption.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.adoption.dto.AdoptionPostCreateDto;
import com.sns.marigold.adoption.dto.AdoptionPostDetailDto;
import com.sns.marigold.adoption.dto.AdoptionPostDto;
import com.sns.marigold.adoption.dto.AdoptionPostSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionPostUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.entity.AdoptionPostImage;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.exception.AdoptionPostException;
import com.sns.marigold.adoption.repository.AdoptionAdopterRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentImageRepository;
import com.sns.marigold.adoption.repository.AdoptionCommentRepository;
import com.sns.marigold.adoption.repository.AdoptionPostImageRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.storage.StorageFileFixtures;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.objectstore.ObjectStorage;
import com.sns.marigold.storage.service.StorageFileService;
import com.sns.marigold.storage.service.StorageUploadService;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdoptionPostServiceTest {

  @Mock private UserService userService;

  @Mock private ObjectStorage objectStorage;

  @Mock private StorageUploadService storageUploadService;

  @Mock private StorageFileService storageFileService;

  @Mock private AdoptionCommentService adoptionCommentService;

  @Mock private ChatService chatService;

  @Mock private AdoptionPostRepository adoptionPostRepository;

  @Mock private AdoptionAdopterRepository adoptionAdopterRepository;

  @Mock private AdoptionCommentRepository adoptionCommentRepository;

  @Mock private AdoptionCommentImageRepository adoptionCommentImageRepository;

  @Mock private AdoptionPostImageRepository adoptionPostImageRepository;

  @Mock private ChatRoomRepository chatRoomRepository;

  @Mock private RoomParticipantRepository participantRepository;

  @Mock private TransactionTemplate transactionTemplate;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private AuditLogger auditLogger;

  @InjectMocks private AdoptionPostService adoptionPostService;

  private User testUser;
  private AdoptionPost testPost;
  private List<AdoptionPostImage> testImages;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient()
        .when(storageFileService.attach(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(StorageFileFixtures.attachConfirmed());

    testUser =
        User.builder()
            .id(1L)
            .providerInfo(ProviderInfo.KAKAO)
            .providerId("12345")
            .nickname("tester")
            .role(Role.ROLE_PERSON)
            .build();

    testImages =
        new ArrayList<>(
            List.of(
                AdoptionPostImage.builder()
                    .storageFile(
                        StorageFileFixtures.attached(
                            "adoption/post/11111111-1111-1111-1111-111111111111.jpg", "old1.jpg"))
                    .build(),
                AdoptionPostImage.builder()
                    .storageFile(
                        StorageFileFixtures.attached(
                            "adoption/post/22222222-2222-2222-2222-222222222222.jpg", "old2.jpg"))
                    .build()));

    testPost =
        AdoptionPost.builder()
            .writer(testUser)
            .title("Test Title")
            .species(Species.DOG)
            .sex(Sex.MALE)
            .age(2)
            .weight(5.0)
            .area("Seoul")
            .neutering(Neutering.YES)
            .features("Cute dog")
            .images(testImages)
            .build();

    // ReflectionTestUtils.setField(testPost, "id", 100L); // 만약 id 세팅이 필요한 경우

    // TransactionTemplate Mocking
    lenient()
        .doAnswer(
            invocation -> {
              Consumer<TransactionStatus> action = invocation.getArgument(0);
              action.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());

    lenient()
        .doAnswer(
            invocation -> {
              TransactionCallback<?> action = invocation.getArgument(0);
              return action.doInTransaction(null);
            })
        .when(transactionTemplate)
        .execute(any());
  }

  @Test
  @DisplayName("게시글 생성 시 확정한 이미지로 저장되고 ID를 반환한다.")
  void create_Success() {
    // given
    AdoptionPostCreateDto dto =
        AdoptionPostCreateDto.builder()
            .title("New Post")
            .species(Species.CAT)
            .sex(Sex.FEMALE)
            .age(1)
            .weight(3.0)
            .area("Busan")
            .neutering(Neutering.UNKNOWN)
            .features("Lovely cat")
            .uploadTokens(List.of("token-1"))
            .build();

    given(userService.findEntityById(1L)).willReturn(testUser);
    given(storageUploadService.confirm(1L, UploadPurpose.ADOPTION_POST_IMAGE, List.of("token-1")))
        .willReturn(
            List.of(
                confirmedImage(
                    "adoption/post/33333333-3333-3333-3333-333333333333.jpg", "original.jpg")));

    AdoptionPost savedPost = mock(AdoptionPost.class);
    given(savedPost.getId()).willReturn(100L);
    given(adoptionPostRepository.save(any(AdoptionPost.class))).willReturn(savedPost);

    // when
    Long postId = adoptionPostService.create(dto, 1L);

    // then
    assertThat(postId).isEqualTo(100L);

    // 저장될 때 AdoptionPost 엔티티에 Image 객체가 제대로 생성되어 들어갔는지 검증
    ArgumentCaptor<AdoptionPost> postCaptor = ArgumentCaptor.forClass(AdoptionPost.class);
    verify(adoptionPostRepository, times(1)).save(postCaptor.capture());

    AdoptionPost capturedPost = postCaptor.getValue();
    assertThat(capturedPost.getImages()).hasSize(1);
    assertThat(capturedPost.getImages().get(0).getOriginalFileName()).isEqualTo("original.jpg");
    assertThat(capturedPost.getImages().get(0).getStoredFileName())
        .isEqualTo("adoption/post/33333333-3333-3333-3333-333333333333.jpg");
    verifyNoInteractions(eventPublisher, objectStorage);
  }

  @Test
  @DisplayName("게시글 생성 시 이미지가 없으면 확정 전에 실패한다.")
  void create_WithoutImages() {
    AdoptionPostCreateDto dto = AdoptionPostCreateDto.builder().uploadTokens(List.of()).build();

    assertThatThrownBy(() -> adoptionPostService.create(dto, 1L))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forInvalidPostImages().getMessage());

    verifyNoInteractions(storageUploadService, userService, adoptionPostRepository);
  }

  @Test
  @DisplayName("사용자 조회에 실패하면 업로드를 확정하지 않는다.")
  void create_UserNotFoundDoesNotConfirmUploads() {
    AdoptionPostCreateDto dto =
        AdoptionPostCreateDto.builder().uploadTokens(List.of("token-1")).build();
    UserException failure = UserException.forUserNotFound();
    given(userService.findEntityById(1L)).willThrow(failure);

    assertThatThrownBy(() -> adoptionPostService.create(dto, 1L)).isSameAs(failure);

    verifyNoInteractions(storageUploadService, eventPublisher, transactionTemplate);
  }

  @Test
  @DisplayName("업로드 확정에 실패하면 트랜잭션을 시작하지 않는다.")
  void create_ConfirmFailureDoesNotStartTransaction() {
    AdoptionPostCreateDto dto =
        AdoptionPostCreateDto.builder().uploadTokens(List.of("token-1")).build();
    given(userService.findEntityById(1L)).willReturn(testUser);
    StorageException failure = StorageException.forInvalidUploadToken("expired");
    given(storageUploadService.confirm(anyLong(), any(), any())).willThrow(failure);

    assertThatThrownBy(() -> adoptionPostService.create(dto, 1L)).isSameAs(failure);

    verifyNoInteractions(transactionTemplate, adoptionPostRepository);
  }

  @Test
  @DisplayName("확정 후 트랜잭션이 실패해도 확정한 파일을 지우지 않고 고아 정리에 맡긴다.")
  void create_TransactionFailureKeepsConfirmedFiles() {
    AdoptionPostCreateDto dto =
        AdoptionPostCreateDto.builder().uploadTokens(List.of("token-1")).build();
    given(userService.findEntityById(1L)).willReturn(testUser);
    given(storageUploadService.confirm(anyLong(), any(), any()))
        .willReturn(
            List.of(
                confirmedImage(
                    "adoption/post/33333333-3333-3333-3333-333333333333.jpg", "new.jpg")));
    IllegalStateException failure = new IllegalStateException("transaction start failed");
    doThrow(failure).when(transactionTemplate).execute(any());

    assertThatThrownBy(() -> adoptionPostService.create(dto, 1L)).isSameAs(failure);

    verifyNoInteractions(objectStorage);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("게시글 수정 시 작성자가 일치하면 정상적으로 수정된다.")
  void update_Success() {
    // given
    AdoptionPostUpdateDto dto =
        AdoptionPostUpdateDto.builder()
            .title("Updated Title")
            .species(Species.DOG)
            .sex(Sex.MALE)
            .age(3)
            .weight(6.0)
            .area("Seoul")
            .neutering(Neutering.YES)
            .features("Updated features")
            .imagesToKeep(List.of("adoption/post/11111111-1111-1111-1111-111111111111.jpg"))
            .uploadTokens(List.of("token-1"))
            .build();

    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
    given(storageUploadService.confirm(1L, UploadPurpose.ADOPTION_POST_IMAGE, List.of("token-1")))
        .willReturn(
            List.of(
                confirmedImage(
                    "adoption/post/44444444-4444-4444-4444-444444444444.jpg", "original.jpg")));

    // when
    adoptionPostService.update(100L, 1L, dto);

    // then
    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);

    assertThat(testPost.getTitle()).isEqualTo("Updated Title");
    assertThat(testPost.getAge()).isEqualTo(3);
    assertThat(testPost.getImages()).hasSize(2);
    assertThat(testPost.getImages().get(0).getStoredFileName())
        .isEqualTo("adoption/post/11111111-1111-1111-1111-111111111111.jpg");
    assertThat(testPost.getImages().get(1).getStoredFileName())
        .isEqualTo("adoption/post/44444444-4444-4444-4444-444444444444.jpg");
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

    assertThat(eventCaptor.getValue().fileNames())
        .contains("adoption/post/22222222-2222-2222-2222-222222222222.jpg");
  }

  @Test
  @DisplayName("게시글 수정 시 존재하지 않는 유지 이미지 파일명이 있으면 실패한다.")
  void update_InvalidImageToKeep() {
    // given
    AdoptionPostUpdateDto dto =
        AdoptionPostUpdateDto.builder()
            .title("Updated Title")
            .species(Species.DOG)
            .sex(Sex.MALE)
            .age(3)
            .weight(6.0)
            .area("Seoul")
            .neutering(Neutering.YES)
            .features("Updated features")
            .imagesToKeep(List.of("adoption/post/99999999-9999-9999-9999-999999999999.jpg"))
            .uploadTokens(List.of())
            .build();

    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(() -> adoptionPostService.update(100L, 1L, dto))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forInvalidPostImages().getMessage());

    assertThat(testPost.getImages())
        .extracting(AdoptionPostImage::getStoredFileName)
        .containsExactly(
            "adoption/post/11111111-1111-1111-1111-111111111111.jpg",
            "adoption/post/22222222-2222-2222-2222-222222222222.jpg");
    verifyNoInteractions(storageUploadService);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("게시글 수정 시 작성자가 다르면 AuthException이 발생한다.")
  void update_NotWriter() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(() -> adoptionPostService.update(100L, 2L, new AdoptionPostUpdateDto()))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forAccessDenied().getMessage());
  }

  @Test
  @DisplayName("업로드를 확정하는 동안 게시글이 삭제되면 수정을 반영하지 않는다.")
  void update_DeletedWhileConfirming() {
    // given: 사전 검사 때는 살아 있고, 저장 트랜잭션에서 다시 읽을 때는 삭제된 게시글
    AdoptionPost deletedPost =
        AdoptionPost.builder()
            .writer(testUser)
            .title("Test Title")
            .images(new ArrayList<>(testImages))
            .build();
    deletedPost.softDelete();
    given(adoptionPostRepository.findById(100L))
        .willReturn(Optional.of(testPost), Optional.of(deletedPost));
    AdoptionPostUpdateDto dto =
        AdoptionPostUpdateDto.builder()
            .title("Updated Title")
            .imagesToKeep(testImages.stream().map(AdoptionPostImage::getStoredFileName).toList())
            .build();

    // when & then
    assertThatThrownBy(() -> adoptionPostService.update(100L, 1L, dto))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forAdoptionPostDeleted().getMessage());
    assertThat(testPost.getTitle()).isEqualTo("Test Title");
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("목록 조회 시 대표 이미지 URL을 만들 수 없어도 게시글은 반환한다.")
  void search_ImageNotFound() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    String storedFileName = testImages.get(0).getStoredFileName();
    given(adoptionPostRepository.findAll(any(Specification.class), eq(pageable)))
        .willReturn(new PageImpl<>(List.of(testPost), pageable, 1));
    given(objectStorage.getViewUrlOrNull(storedFileName))
        .willThrow(StorageException.forFileNotFound());

    // when
    Page<AdoptionPostDto> result =
        adoptionPostService.search(new AdoptionPostSearchFilterDto(), pageable);

    // then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getImageUrl()).isNull();
  }

  @Test
  @DisplayName("상세 조회 시 이미지 URL을 만들 수 없어도 파일명과 URL 인덱스를 유지한다.")
  void getDetail_ImageNotFound() {
    // given
    String missingStoredFileName = testImages.get(0).getStoredFileName();
    String storedFileName = testImages.get(1).getStoredFileName();
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
    given(objectStorage.getViewUrlOrNull(missingStoredFileName))
        .willThrow(StorageException.forFileNotFound());
    given(objectStorage.getViewUrlOrNull(storedFileName))
        .willReturn("http://example.com/image.jpg");

    // when
    AdoptionPostDetailDto result = adoptionPostService.getDetail(100L);

    // then
    assertThat(result.getImageFileNames()).containsExactly(missingStoredFileName, storedFileName);
    assertThat(result.getImageUrls()).containsExactly(null, "http://example.com/image.jpg");
  }

  @Test
  @DisplayName("게시글 상태 업데이트 시 정상적으로 반영된다.")
  void updateRecruitmentStatus_Success() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when
    adoptionPostService.updateRecruitmentStatus(100L, AdoptionPostStatus.RESERVED, 1L);

    // then
    assertThat(testPost.getStatus()).isEqualTo(AdoptionPostStatus.RESERVED);
  }

  @Test
  @DisplayName("일반 상태 변경으로 입양 완료를 요청하면 기존 상태를 유지하고 거부한다.")
  void updateRecruitmentStatus_CompletedRejected() {
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
    AdoptionPostStatus previousStatus = testPost.getStatus();

    assertThatThrownBy(
            () ->
                adoptionPostService.updateRecruitmentStatus(100L, AdoptionPostStatus.COMPLETED, 1L))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessage(AdoptionPostException.forAdoptionCompletionRequired().getMessage());

    assertThat(testPost.getStatus()).isEqualTo(previousStatus);
    verifyNoInteractions(adoptionAdopterRepository);
  }

  @Test
  @DisplayName("작성자가 아니라면 게시글 상태 업데이트가 불가능하다.")
  void updateRecruitmentStatus_NotWriter() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(
            () ->
                adoptionPostService.updateRecruitmentStatus(100L, AdoptionPostStatus.COMPLETED, 2L))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forAccessDenied().getMessage());
  }

  @Test
  @DisplayName("탈퇴한 작성자의 게시글을 논리 삭제하고 게시글 이미지 삭제 이벤트를 발행한다.")
  void deleteAllByWriter_Success() {
    String imageFile = "adoption/post/44444444-4444-4444-4444-444444444444.jpg";
    given(adoptionPostImageRepository.findStoredFileNamesByWriter(1L))
        .willReturn(List.of(imageFile));

    adoptionPostService.deleteAllByWriter(1L);

    verify(adoptionPostRepository).setDeletedTimeByWriter(1L);
    verify(adoptionPostImageRepository).deleteImagesByWriter(1L);
    verify(eventPublisher).publishEvent(new DeleteOldStorageFilesEvent(List.of(imageFile)));
  }

  @Test
  @DisplayName("게시글 삭제 시 원문, 댓글, 이미지가 삭제되고 연관 채팅방이 종료된다.")
  void delete_Success() {
    // given
    testPost.addImage(
        AdoptionPostImage.builder()
            .storageFile(
                StorageFileFixtures.attached(
                    "adoption/post/55555555-5555-5555-5555-555555555555.jpg", "image.jpg"))
            .build());
    testPost.addImage(
        AdoptionPostImage.builder()
            .storageFile(
                StorageFileFixtures.attached(
                    "adoption/post/66666666-6666-6666-6666-666666666666.jpg", "image.jpg"))
            .build());
    testPost.addImage(
        AdoptionPostImage.builder()
            .storageFile(
                StorageFileFixtures.attached(
                    "adoption/post/77777777-7777-7777-7777-777777777777.jpg", "image.jpg"))
            .build());

    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when
    adoptionPostService.delete(100L, 1L);

    // then
    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    assertThat(testPost.getDeletedAt()).isNotNull();
    assertThat(testPost.getTitle()).isEqualTo("삭제된 게시글입니다");
    assertThat(testPost.getFeatures()).isEqualTo("삭제된 게시글입니다");
    assertThat(testPost.getArea()).isEqualTo("비공개");
    assertThat(testPost.getImages()).isEmpty();
    verify(adoptionCommentService, times(1)).deleteCommentsByPostId(100L);
    verify(chatService, times(1)).closeAllChatRoomsByPostId(100L);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames()).hasSize(5);
    assertThat(eventCaptor.getValue().fileNames())
        .contains(
            "adoption/post/11111111-1111-1111-1111-111111111111.jpg",
            "adoption/post/22222222-2222-2222-2222-222222222222.jpg",
            "adoption/post/55555555-5555-5555-5555-555555555555.jpg",
            "adoption/post/66666666-6666-6666-6666-666666666666.jpg",
            "adoption/post/77777777-7777-7777-7777-777777777777.jpg");
  }

  @Test
  @DisplayName("삭제된 게시글을 다시 삭제할 수 없다.")
  void delete_AlreadyDeleted() {
    // given
    testPost.softDelete();
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(() -> adoptionPostService.delete(100L, 1L))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forAdoptionPostDeleted().getMessage());
    verify(adoptionCommentService, never()).deleteCommentsByPostId(any());
    verify(chatService, never()).closeAllChatRoomsByPostId(any());
  }

  @Test
  @DisplayName("작성자가 아니라면 게시글을 삭제할 수 없다.")
  void delete_NotWriter() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(() -> adoptionPostService.delete(100L, 2L))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forAccessDenied().getMessage());
    verify(adoptionCommentService, never()).deleteCommentsByPostId(any());
    verify(chatService, never()).closeAllChatRoomsByPostId(any());
  }

  @Test
  @DisplayName("채팅 참여자인 후보를 입양자로 지정하면 게시글을 입양 완료로 바꾸고 입양자를 저장한다.")
  void completeAdoption_Success() {
    // given
    User adopter = User.builder().id(2L).nickname("adopter").role(Role.ROLE_PERSON).build();
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
    given(participantRepository.existsByChatRoom_AdoptionPost_IdAndUser_Id(100L, 2L))
        .willReturn(true);
    given(userService.findEntityById(2L)).willReturn(adopter);

    // when
    adoptionPostService.completeAdoption(100L, 2L, 1L);

    // then
    assertThat(testPost.getStatus()).isEqualTo(AdoptionPostStatus.COMPLETED);
    verify(adoptionAdopterRepository, times(1)).save(any());
  }

  @Test
  @DisplayName("게시글 채팅 참여자가 아닌 사용자는 입양자로 지정할 수 없다.")
  void completeAdoption_NotCandidate() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
    given(participantRepository.existsByChatRoom_AdoptionPost_IdAndUser_Id(100L, 3L))
        .willReturn(false);

    // when & then
    assertThatThrownBy(() -> adoptionPostService.completeAdoption(100L, 3L, 1L))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forAdopterNotCandidate().getMessage());
    assertThat(testPost.getStatus()).isEqualTo(AdoptionPostStatus.PROCEEDING);
    verify(adoptionAdopterRepository, never()).save(any());
  }

  @Test
  @DisplayName("작성자 본인은 입양자로 지정할 수 없다.")
  void completeAdoption_WriterAsAdopter() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(() -> adoptionPostService.completeAdoption(100L, 1L, 1L))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forAdopterNotCandidate().getMessage());
    verify(adoptionAdopterRepository, never()).save(any());
  }

  @Test
  @DisplayName("입양 확정 취소 시 입양자 엔터티를 삭제하고 게시글 상태를 진행 중으로 되돌린다.")
  void cancelAdoption_Success() {
    // given
    testPost.updateStatus(AdoptionPostStatus.COMPLETED);
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when
    adoptionPostService.cancelAdoption(100L, 1L);

    // then
    assertThat(testPost.getStatus()).isEqualTo(AdoptionPostStatus.PROCEEDING);
    verify(adoptionAdopterRepository, times(1)).deleteByAdoptionPostId(100L);
  }

  @Test
  @DisplayName("입양 완료 상태가 아니라면 입양자 엔터티를 삭제할 수 없다.")
  void cancelAdoption_NotCompleted() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(() -> adoptionPostService.cancelAdoption(100L, 1L))
        .isInstanceOf(AdoptionPostException.class)
        .hasMessageContaining(AdoptionPostException.forAdoptionPostNotCompleted().getMessage());
    verify(adoptionAdopterRepository, never()).deleteByAdoptionPostId(any());
  }

  private static FileUploadDto confirmedImage(String storedFileName, String originalFileName) {
    return FileUploadDto.builder()
        .storedFileName(storedFileName)
        .originalFileName(originalFileName)
        .contentType("image/jpeg")
        .fileSize(1L)
        .build();
  }
}
