package com.sns.marigold.adoption.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

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
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.StorageDirectory;
import com.sns.marigold.storage.service.StorageService;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class AdoptionPostServiceTest {

  @Mock private UserService userService;

  @Mock private StorageService storageService;

  @Mock private AdoptionCommentService adoptionCommentService;

  @Mock private ChatService chatService;

  @Mock private AdoptionPostRepository adoptionPostRepository;

  @Mock private AdoptionAdopterRepository adoptionAdopterRepository;

  @Mock private AdoptionCommentRepository adoptionCommentRepository;

  @Mock private AdoptionCommentImageRepository adoptionCommentImageRepository;

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
                    .storedFileName("adoption/post/11111111-1111-1111-1111-111111111111.jpg")
                    .originalFileName("old1.jpg")
                    .build(),
                AdoptionPostImage.builder()
                    .storedFileName("adoption/post/22222222-2222-2222-2222-222222222222.jpg")
                    .originalFileName("old2.jpg")
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
  @DisplayName("게시글 생성 시 정상적으로 저장되고 ID를 반환한다.")
  void create_Success() {
    // given
    List<MultipartFile> multipartFiles =
        List.of(
            new MockMultipartFile(
                "file", "original.jpg", MediaType.IMAGE_PNG_VALUE, "test".getBytes()));

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
            .images(multipartFiles)
            .build();

    given(userService.findEntityById(1L)).willReturn(testUser);
    given(storageService.uploadImages(any(), eq(StorageDirectory.ADOPTION_POST)))
        .willReturn(
            List.of(
                ImageUploadDto.builder()
                    .originalFileName("original.jpg")
                    .storedFileName("adoption/post/33333333-3333-3333-3333-333333333333.jpg")
                    .build()));

    AdoptionPost savedPost = mock(AdoptionPost.class);
    given(savedPost.getId()).willReturn(100L);
    given(adoptionPostRepository.save(any(AdoptionPost.class))).willReturn(savedPost);

    // when
    Long postId = adoptionPostService.create(dto, 1L);

    // then
    assertThat(postId).isEqualTo(100L);
    verify(storageService, times(1))
        .uploadImages(
            multipartFiles, StorageDirectory.ADOPTION_POST); // 스토리지 서비스에 정확한 파일이 전달되었는지 검증

    // 저장될 때 AdoptionPost 엔티티에 Image 객체가 제대로 생성되어 들어갔는지 검증
    ArgumentCaptor<AdoptionPost> postCaptor = ArgumentCaptor.forClass(AdoptionPost.class);
    verify(adoptionPostRepository, times(1)).save(postCaptor.capture());

    AdoptionPost capturedPost = postCaptor.getValue();
    assertThat(capturedPost.getImages()).hasSize(1);
    assertThat(capturedPost.getImages().get(0).getOriginalFileName()).isEqualTo("original.jpg");
    assertThat(capturedPost.getImages().get(0).getStoredFileName())
        .isEqualTo("adoption/post/33333333-3333-3333-3333-333333333333.jpg");
  }

  @Test
  @DisplayName("게시글 수정 시 작성자가 일치하면 정상적으로 수정된다.")
  void update_Success() {
    List<MultipartFile> multipartFiles =
        List.of(
            new MockMultipartFile(
                "file", "original.jpg", MediaType.IMAGE_PNG_VALUE, "test".getBytes()));

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
            .images(multipartFiles)
            .build();

    List<ImageUploadDto> uploadedImages =
        List.of(
            ImageUploadDto.builder()
                .originalFileName("original.jpg")
                .storedFileName("adoption/post/44444444-4444-4444-4444-444444444444.jpg")
                .build());

    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
    given(storageService.uploadImages(any(), eq(StorageDirectory.ADOPTION_POST)))
        .willReturn(uploadedImages);

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
            .images(List.of())
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
    verify(storageService, never()).uploadImages(any(), any());
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
  @DisplayName("목록 조회 시 대표 이미지 URL을 만들 수 없어도 게시글은 반환한다.")
  void search_ImageNotFound() {
    // given
    Pageable pageable = PageRequest.of(0, 10);
    String storedFileName = testImages.get(0).getStoredFileName();
    given(adoptionPostRepository.findAll(any(Specification.class), eq(pageable)))
        .willReturn(new PageImpl<>(List.of(testPost), pageable, 1));
    given(storageService.getViewUrlOrNull(storedFileName))
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
    given(storageService.getViewUrlOrNull(missingStoredFileName))
        .willThrow(StorageException.forFileNotFound());
    given(storageService.getViewUrlOrNull(storedFileName)).willReturn("http://example.com/image.jpg");

    // when
    AdoptionPostDetailDto result = adoptionPostService.getDetail(100L);

    // then
    assertThat(result.getImageFileNames()).containsExactly(missingStoredFileName, storedFileName);
    assertThat(result.getImageUrls()).containsExactly(null, "http://example.com/image.jpg");
  }

  @Test
  @DisplayName("게시글 상태 업데이트 시 정상적으로 반영된다.")
  void updateStatus_Success() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when
    adoptionPostService.updateStatus(100L, AdoptionPostStatus.COMPLETED, 1L);

    // then
    assertThat(testPost.getStatus()).isEqualTo(AdoptionPostStatus.COMPLETED);
  }

  @Test
  @DisplayName("작성자가 아니라면 게시글 상태 업데이트가 불가능하다.")
  void updateStatus_NotWriter() {
    // given
    given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));

    // when & then
    assertThatThrownBy(
            () -> adoptionPostService.updateStatus(100L, AdoptionPostStatus.COMPLETED, 2L))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forAccessDenied().getMessage());
  }

  @Test
  @DisplayName("게시글 삭제 시 원문, 댓글, 이미지가 삭제되고 연관 채팅방이 종료된다.")
  void delete_Success() {
    // given
    testPost.addImage(
        AdoptionPostImage.builder()
            .storedFileName("adoption/post/55555555-5555-5555-5555-555555555555.jpg")
            .build());
    testPost.addImage(
        AdoptionPostImage.builder()
            .storedFileName("adoption/post/66666666-6666-6666-6666-666666666666.jpg")
            .build());
    testPost.addImage(
        AdoptionPostImage.builder()
            .storedFileName("adoption/post/77777777-7777-7777-7777-777777777777.jpg")
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
}
