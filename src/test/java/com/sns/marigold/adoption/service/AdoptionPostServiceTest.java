package com.sns.marigold.adoption.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.sns.marigold.adoption.dto.AdoptionPostCreateDto;
import com.sns.marigold.adoption.dto.AdoptionPostUpdateDto;
import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.entity.AdoptionPostImage;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.adoption.repository.AdoptionAdopterRepository;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.service.UserService;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AdoptionPostServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private S3Service s3Service;

    @Mock
    private AdoptionPostRepository adoptionPostRepository;

    @Mock
    private AdoptionAdopterRepository adoptionAdopterRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoomParticipantRepository participantRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdoptionPostService adoptionPostService;

    private User testUser;
    private AdoptionPost testPost;
    private List<AdoptionPostImage> testImages;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .providerInfo(ProviderInfo.KAKAO)
                .providerId("12345")
                .nickname("tester")
                .role(Role.ROLE_PERSON)
                .build();

        testImages = new ArrayList<>(List.of(
                AdoptionPostImage.builder()
                        .storedFileName("old1.jpg")
                        .originalFileName("old1.jpg")
                        .build(),
                AdoptionPostImage.builder()
                        .storedFileName("old2.jpg")
                        .originalFileName("old2.jpg")
                        .build()));

        testPost = AdoptionPost.builder()
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
        List<MultipartFile> multipartFiles = List.of(
                new MockMultipartFile(
                        "file", "original.jpg", MediaType.IMAGE_PNG_VALUE, "test".getBytes()));

        AdoptionPostCreateDto dto = AdoptionPostCreateDto.builder()
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
        given(s3Service.uploadImagesToS3(any()))
                .willReturn(
                        List.of(
                                ImageUploadDto.builder()
                                        .originalFileName("original.jpg")
                                        .storedFileName("stored.jpg")
                                        .build()));

        AdoptionPost savedPost = mock(AdoptionPost.class);
        given(savedPost.getId()).willReturn(100L);
        given(adoptionPostRepository.save(any(AdoptionPost.class))).willReturn(savedPost);

        // when
        Long postId = adoptionPostService.create(dto, 1L);

        // then
        assertThat(postId).isEqualTo(100L);
        verify(s3Service, times(1)).uploadImagesToS3(multipartFiles); // S3 서비스에 정확한 파일이 전달되었는지 검증

        // 저장될 때 AdoptionPost 엔티티에 Image 객체가 제대로 생성되어 들어갔는지 검증
        ArgumentCaptor<AdoptionPost> postCaptor = ArgumentCaptor.forClass(AdoptionPost.class);
        verify(adoptionPostRepository, times(1)).save(postCaptor.capture());

        AdoptionPost capturedPost = postCaptor.getValue();
        assertThat(capturedPost.getImages()).hasSize(1);
        assertThat(capturedPost.getImages().get(0).getOriginalFileName()).isEqualTo("original.jpg");
        assertThat(capturedPost.getImages().get(0).getStoredFileName()).isEqualTo("stored.jpg");
    }

    @Test
    @DisplayName("게시글 수정 시 작성자가 일치하면 정상적으로 수정된다.")
    void update_Success() {
        List<MultipartFile> multipartFiles = List.of(
                new MockMultipartFile(
                        "file", "original.jpg", MediaType.IMAGE_PNG_VALUE, "test".getBytes()));

        // given
        AdoptionPostUpdateDto dto = AdoptionPostUpdateDto.builder()
                .title("Updated Title")
                .species(Species.DOG)
                .sex(Sex.MALE)
                .age(3)
                .weight(6.0)
                .area("Seoul")
                .neutering(Neutering.YES)
                .features("Updated features")
                .imagesToKeep(List.of("old1.jpg"))
                .images(multipartFiles)
                .build();

        List<ImageUploadDto> uploadedImages = List.of(
                ImageUploadDto.builder()
                        .originalFileName("original.jpg")
                        .storedFileName("old3.jpg")
                        .build());

        given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
        given(s3Service.uploadImagesToS3(any())).willReturn(uploadedImages);

        // when
        adoptionPostService.update(100L, 1L, dto);

        // then
        ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor = ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);

        assertThat(testPost.getTitle()).isEqualTo("Updated Title");
        assertThat(testPost.getAge()).isEqualTo(3);
        assertThat(testPost.getImages()).hasSize(2);
        assertThat(testPost.getImages().get(0).getStoredFileName()).isEqualTo("old1.jpg");
        assertThat(testPost.getImages().get(1).getStoredFileName()).isEqualTo("old3.jpg");
        verify(eventPublisher, times(1))
                .publishEvent(eventCaptor.capture()); // old2.jpg 삭제 이벤트 발생

        assertThat(eventCaptor.getValue().fileNames()).contains("old2.jpg");
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
        assertThatThrownBy(() -> adoptionPostService.updateStatus(100L, AdoptionPostStatus.COMPLETED, 2L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(AuthException.forAccessDenied().getMessage());
    }

    @Test
    @DisplayName("게시글 삭제 시 상태가 변경되고 연관 채팅방이 종료된다.")
    void delete_Success() {
        // given
        testPost.addImage(AdoptionPostImage.builder().storedFileName("img1.jpg").build());
        testPost.addImage(AdoptionPostImage.builder().storedFileName("img2.jpg").build());
        testPost.addImage(AdoptionPostImage.builder().storedFileName("img3.jpg").build());

        given(adoptionPostRepository.findById(100L)).willReturn(Optional.of(testPost));
        given(chatRoomRepository.findAllByAdoptionPostId(100L)).willReturn(List.of());

        // when
        adoptionPostService.delete(100L, 1L);

        // then
        ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor = ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class); 
        assertThat(testPost.getDeletedAt()).isNotNull();
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().fileNames()).hasSize(4);
        assertThat(eventCaptor.getValue().fileNames()).contains("img1.jpg", "img2.jpg", "img3.jpg", "old2.jpg");
        assertThat(eventCaptor.getValue().fileNames()).doesNotContain("old1.jpg");
    }
}
