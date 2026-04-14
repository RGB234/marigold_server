package com.sns.marigold.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.entity.UserImage;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private AdoptionPostRepository adoptionPostRepository;

  @Mock private ChatRoomRepository chatRoomRepository;

  @Mock private S3Service s3Service;

  @Mock private TransactionTemplate transactionTemplate;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private UserService userService;

  private User testUser;

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
  @DisplayName("존재하는 사용자 ID로 조회 시 정상 반환한다.")
  void findEntityById_Success() {
    // given
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    // when
    User result = userService.findEntityById(1L);

    // then
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getNickname()).isEqualTo("tester");
  }

  @Test
  @DisplayName("존재하지 않는 사용자 ID로 조회 시 UserException이 발생한다.")
  void findEntityById_NotFound() {
    // given
    given(userRepository.findById(999L)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userService.findEntityById(999L))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserNotFound().getMessage());
  }

  @Test
  @DisplayName("이미지를 제외한 나머지 계정 정보를 업데이트한다.")
  void updateUser_WithoutImage_Success() {
    // given
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    dto.setRemoveImage(false);

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    verify(s3Service, never()).uploadFile(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("사용자 프로필 이미지를 새로 업로드한다.")
  void updateUser_NewImage_Success() {
    // given
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    MockMultipartFile file =
        new MockMultipartFile(
            "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".getBytes());
    dto.setImage(file);

    ImageUploadDto imageUploadDto = new ImageUploadDto("stored.jpg", "test.jpg");
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(s3Service.uploadFile(any(MultipartFile.class))).willReturn(imageUploadDto);

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    assertThat(testUser.getImage()).isNotNull();
    assertThat(testUser.getImage().getStoredFileName()).isEqualTo("stored.jpg");

    verify(s3Service, times(1)).uploadFile(any(MultipartFile.class));
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("사용자 프로필 이미지를 교체한다.")
  void updateUser_ReplaceImage_Success() {
    // given
    testUser.update(
        "tester",
        UserImage.builder().storedFileName("old.jpg").originalFileName("old.jpg").build());

    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    MockMultipartFile file =
        new MockMultipartFile(
            "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".getBytes());
    dto.setImage(file);

    ImageUploadDto imageUploadDto = new ImageUploadDto("new.jpg", "test.jpg");
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(s3Service.uploadFile(any(MultipartFile.class))).willReturn(imageUploadDto);

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    assertThat(testUser.getImage().getStoredFileName()).isEqualTo("new.jpg");

    verify(s3Service, times(1)).uploadFile(any(MultipartFile.class));

    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames()).contains("old.jpg");
  }

  @Test
  @DisplayName("사용자 프로필 이미지를 기본 이미지로 변경(삭제)한다.")
  void updateUser_RemoveImage_Success() {
    // given
    testUser.update(
        "tester",
        UserImage.builder().storedFileName("old.jpg").originalFileName("old.jpg").build());

    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    dto.setRemoveImage(true);

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    assertThat(testUser.getImage()).isNull();

    verify(s3Service, never()).uploadFile(any());

    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames()).contains("old.jpg");
  }

  @Test
  @DisplayName("회원 탈퇴(소프트 딜리트) 시 작성한 게시글을 논리 삭제하고 참여 중인 채팅방을 닫는다.")
  void deleteUser_Success() {
    // given
    testUser.update(
        "tester",
        UserImage.builder().storedFileName("old.jpg").originalFileName("old.jpg").build());
    ChatRoom chatRoom = ChatRoom.builder().id(200L).status(ChatRoomStatus.ACTIVE).build();

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(chatRoomRepository.findAllByUser(eq(testUser), any(Pageable.class)))
        .willReturn(new PageImpl<>(List.of(chatRoom)));

    // when
    userService.deleteUser(1L, 1L);

    // then
    verify(adoptionPostRepository, times(1)).softDeleteByWriter(1L);
    assertThat(testUser.getStatus().name()).isEqualTo("DELETED");
    assertThat(chatRoom.getStatus()).isEqualTo(ChatRoomStatus.CLOSED); // 채팅방 종료 검증
    verify(userRepository, times(1)).save(testUser);
    verify(eventPublisher, times(1))
        .publishEvent(any(DeleteOldStorageFilesEvent.class)); // 유저 프로필 이미지가 존재할 경우 이미지 삭제 이벤트 발생
  }
}
