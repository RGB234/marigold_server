package com.sns.marigold.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.tsid.TsidCodec;
import com.sns.marigold.storage.StorageFileFixtures;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.UploadToken;
import com.sns.marigold.storage.event.DeleteOldStorageFilesEvent;
import com.sns.marigold.storage.objectstore.ObjectStorage;
import com.sns.marigold.storage.service.StorageFileService;
import com.sns.marigold.storage.service.StorageUploadService;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.dto.update.EmailPasswordRegisterDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.entity.UserImage;
import com.sns.marigold.user.enums.UserStatus;
import com.sns.marigold.user.event.UserDeletedEvent;
import com.sns.marigold.user.exception.UserError;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private ObjectStorage objectStorage;

  @Mock private StorageUploadService storageUploadService;

  @Mock private StorageFileService storageFileService;

  @Mock private TransactionTemplate transactionTemplate;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private AuditLogger auditLogger;

  @Mock private AuthSessionService authSessionService;

  @InjectMocks private UserService userService;

  private User testUser;

  @BeforeEach
  @SuppressWarnings("null")
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

    // TransactionTemplate Mocking
    lenient()
        .doAnswer(
            invocation -> {
              Consumer<TransactionStatus> action = invocation.getArgument(0);
              action.accept(mock(TransactionStatus.class));
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());

    lenient()
        .doAnswer(
            invocation -> {
              TransactionCallback<Object> action = invocation.getArgument(0);
              return action.doInTransaction(mock(TransactionStatus.class));
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
  @DisplayName("OAuth2 가입 사용자는 이메일/비밀번호 로그인을 추가로 등록할 수 있다.")
  void registerEmailAndPassword_Success() {
    User oauthUser = User.builder().id(2L).nickname("oauth-user").role(Role.ROLE_PERSON).build();
    EmailPasswordRegisterDto dto =
        new EmailPasswordRegisterDto("oauth@example.com", "password123!");

    given(userRepository.findById(2L)).willReturn(Optional.of(oauthUser));
    given(userRepository.existsByEmail(dto.getEmail())).willReturn(false);
    given(passwordEncoder.encode(dto.getPassword())).willReturn("encoded-password");

    userService.registerEmailAndPassword(2L, dto);

    assertThat(oauthUser.getEmail()).isEqualTo("oauth@example.com");
    assertThat(oauthUser.getPassword()).isEqualTo("encoded-password");
    verify(passwordEncoder).encode("password123!");
  }

  @Test
  @DisplayName("이미 이메일/비밀번호가 등록된 사용자는 중복 등록할 수 없다.")
  void registerEmailAndPassword_AlreadyRegistered() {
    User localUser =
        User.builder()
            .id(2L)
            .nickname("local-user")
            .email("local@example.com")
            .password("encoded-password")
            .role(Role.ROLE_PERSON)
            .build();
    EmailPasswordRegisterDto dto = new EmailPasswordRegisterDto("new@example.com", "password123!");

    given(userRepository.findById(2L)).willReturn(Optional.of(localUser));

    assertThatThrownBy(() -> userService.registerEmailAndPassword(2L, dto))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserLocalCredentialsAlreadyExists().getMessage());
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("이미 다른 사용자가 사용하는 이메일로는 로그인 정보를 등록할 수 없다.")
  void registerEmailAndPassword_EmailAlreadyExists() {
    User oauthUser = User.builder().id(2L).nickname("oauth-user").role(Role.ROLE_PERSON).build();
    EmailPasswordRegisterDto dto = new EmailPasswordRegisterDto("dup@example.com", "password123!");

    given(userRepository.findById(2L)).willReturn(Optional.of(oauthUser));
    given(userRepository.existsByEmail(dto.getEmail())).willReturn(true);

    assertThatThrownBy(() -> userService.registerEmailAndPassword(2L, dto))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserAlreadyExists().getMessage());
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  @DisplayName("이메일 가입 사용자는 소셜 로그인 정보를 연동할 수 있다.")
  void linkOAuth2_Success() {
    User localUser =
        User.builder()
            .id(2L)
            .nickname("local-user")
            .email("local@example.com")
            .password("encoded-password")
            .role(Role.ROLE_PERSON)
            .build();

    given(userRepository.findById(2L)).willReturn(Optional.of(localUser));
    given(userRepository.existsByProviderInfoAndProviderId(ProviderInfo.NAVER, "naver-123"))
        .willReturn(false);

    userService.linkOAuth2(2L, ProviderInfo.NAVER, "naver-123");

    assertThat(localUser.getProviderInfo()).isEqualTo(ProviderInfo.NAVER);
    assertThat(localUser.getProviderId()).isEqualTo("naver-123");
  }

  @Test
  @DisplayName("이미 소셜 로그인 정보가 연결된 사용자는 중복 연동할 수 없다.")
  void linkOAuth2_AlreadyLinked() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    assertThatThrownBy(() -> userService.linkOAuth2(1L, ProviderInfo.NAVER, "naver-123"))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserOAuth2AlreadyLinked().getMessage());
  }

  @Test
  @DisplayName("이미 다른 사용자에게 연결된 소셜 계정은 연동할 수 없다.")
  void linkOAuth2_AlreadyInUse() {
    User localUser =
        User.builder()
            .id(2L)
            .nickname("local-user")
            .email("local@example.com")
            .password("encoded-password")
            .role(Role.ROLE_PERSON)
            .build();

    given(userRepository.findById(2L)).willReturn(Optional.of(localUser));
    given(userRepository.existsByProviderInfoAndProviderId(ProviderInfo.KAKAO, "12345"))
        .willReturn(true);

    assertThatThrownBy(() -> userService.linkOAuth2(2L, ProviderInfo.KAKAO, "12345"))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserOAuth2AccountAlreadyInUse().getMessage());
  }

  @Test
  @DisplayName("이미지를 제외한 나머지 계정 정보를 업데이트한다.")
  @SuppressWarnings("null")
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
    verifyNoInteractions(storageUploadService);
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("사용자 프로필 이미지를 새로 등록한다.")
  @SuppressWarnings("null")
  void updateUser_NewImage_Success() {
    // given
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    dto.setImageUploadToken(new UploadToken("token-1"));

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(
            storageUploadService.confirm(
                1L, UploadPurpose.USER_PROFILE_IMAGE, List.of(new UploadToken("token-1"))))
        .willReturn(
            List.of(
                confirmedImage(
                    "user/profile/11111111-1111-1111-1111-111111111111.jpg", "test.jpg")));

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    assertThat(testUser.getImage()).isNotNull();
    assertThat(testUser.getImage().getStoredFileName())
        .isEqualTo("user/profile/11111111-1111-1111-1111-111111111111.jpg");

    verify(eventPublisher, never()).publishEvent(any(DeleteOldStorageFilesEvent.class));
    verifyNoInteractions(objectStorage);
  }

  @Test
  @DisplayName("사용자 프로필 이미지를 교체한다.")
  @SuppressWarnings("null")
  void updateUser_ReplaceImage_Success() {
    // given
    testUser.update(
        "tester",
        UserImage.builder()
            .storageFile(
                StorageFileFixtures.attached(
                    "user/profile/22222222-2222-2222-2222-222222222222.jpg", "old.jpg"))
            .build());

    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    dto.setImageUploadToken(new UploadToken("token-1"));

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
    given(
            storageUploadService.confirm(
                1L, UploadPurpose.USER_PROFILE_IMAGE, List.of(new UploadToken("token-1"))))
        .willReturn(
            List.of(
                confirmedImage(
                    "user/profile/33333333-3333-3333-3333-333333333333.jpg", "test.jpg")));

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    assertThat(testUser.getImage().getStoredFileName())
        .isEqualTo("user/profile/33333333-3333-3333-3333-333333333333.jpg");

    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames())
        .contains("user/profile/22222222-2222-2222-2222-222222222222.jpg");
  }

  @Test
  @DisplayName("사용자 프로필 이미지를 기본 이미지로 변경(삭제)한다.")
  @SuppressWarnings("null")
  void updateUser_RemoveImage_Success() {
    // given
    testUser.update(
        "tester",
        UserImage.builder()
            .storageFile(
                StorageFileFixtures.attached(
                    "user/profile/22222222-2222-2222-2222-222222222222.jpg", "old.jpg"))
            .build());

    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    dto.setRemoveImage(true);

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    // when
    userService.updateUser(1L, dto);

    // then
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    assertThat(testUser.getImage()).isNull();

    verifyNoInteractions(storageUploadService);

    ArgumentCaptor<DeleteOldStorageFilesEvent> eventCaptor =
        ArgumentCaptor.forClass(DeleteOldStorageFilesEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().fileNames())
        .contains("user/profile/22222222-2222-2222-2222-222222222222.jpg");
  }

  @Test
  @DisplayName("프로필 삭제와 새 이미지가 함께 입력되면 확정 없이 기존 이미지를 삭제한다.")
  void updateUser_RemoveImageWithUpload_SkipsConfirm() {
    String oldFile = "user/profile/22222222-2222-2222-2222-222222222222.jpg";
    testUser.update(
        "tester",
        UserImage.builder().storageFile(StorageFileFixtures.attached(oldFile, "old.jpg")).build());
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("newNickname");
    dto.setRemoveImage(true);
    dto.setImageUploadToken(new UploadToken("token-1"));
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    userService.updateUser(1L, dto);

    assertThat(testUser.getImage()).isNull();
    assertThat(testUser.getNickname()).isEqualTo("newNickname");
    verifyNoInteractions(objectStorage, storageUploadService);
    verify(eventPublisher).publishEvent(new DeleteOldStorageFilesEvent(List.of(oldFile)));
  }

  @Test
  @DisplayName("회원 탈퇴(소프트 딜리트) 시 개인정보를 지우고 다른 도메인 정리를 위한 탈퇴 이벤트를 발행한다.")
  @SuppressWarnings("null")
  void deleteUser_Success() {
    // given
    testUser.addEmailAndPassword("tester@example.com", "encoded-password");
    testUser.update(
        "tester",
        UserImage.builder()
            .storageFile(
                StorageFileFixtures.attached(
                    "user/profile/22222222-2222-2222-2222-222222222222.jpg", "old.jpg"))
            .build());

    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    // when
    userService.deleteUser(1L);

    // then
    verify(eventPublisher).publishEvent(new UserDeletedEvent(1L));
    assertThat(testUser.getStatus().name()).isEqualTo("DELETED");
    assertThat(testUser.getEmail()).isNull();
    assertThat(testUser.getPassword()).isNull();
    assertThat(testUser.getProviderInfo()).isNull();
    assertThat(testUser.getProviderId()).isNull();
    assertThat(testUser.getImage()).isNull();
    assertThat(testUser.getDeletedAt()).isNotNull();
    assertThat(testUser.getNickname()).isEqualTo("deleted-user-" + TsidCodec.encode(1L));
    verify(eventPublisher)
        .publishEvent(
            new DeleteOldStorageFilesEvent(
                List.of("user/profile/22222222-2222-2222-2222-222222222222.jpg")));
  }

  @Test
  @DisplayName("사용자를 정지하면 상태를 바꾼 트랜잭션이 끝난 뒤 그 사용자의 모든 세션을 폐기한다.")
  void banUser_Success() {
    given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

    userService.banUser(99L, 1L);

    assertThat(testUser.getStatus()).isEqualTo(UserStatus.BANNED);
    InOrder inOrder = inOrder(transactionTemplate, authSessionService);
    inOrder.verify(transactionTemplate).executeWithoutResult(any());
    inOrder.verify(authSessionService).endAll(1L);
  }

  @Test
  @DisplayName("이미 정지된 사용자를 다시 정지해도 세션을 폐기한다. 세션 폐기가 실패했을 때 같은 요청으로 재시도할 수 있어야 한다.")
  void banUser_AlreadyBanned_StillRevokesSessions() {
    User banned = User.builder().id(1L).nickname("banned").status(UserStatus.BANNED).build();
    given(userRepository.findById(1L)).willReturn(Optional.of(banned));

    userService.banUser(99L, 1L);

    assertThat(banned.getStatus()).isEqualTo(UserStatus.BANNED);
    verify(authSessionService).endAll(1L);
  }

  @Test
  @DisplayName("관리자 계정은 정지할 수 없다.")
  void banUser_RejectsAdmin() {
    User admin = User.builder().id(1L).nickname("admin").role(Role.ROLE_ADMIN).build();
    given(userRepository.findById(1L)).willReturn(Optional.of(admin));

    assertThatThrownBy(() -> userService.banUser(99L, 1L))
        .isInstanceOfSatisfying(
            UserException.class,
            e -> assertThat(e.getErrorSpec()).isEqualTo(UserError.ADMIN_CANNOT_BE_BANNED));
    assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
    verifyNoInteractions(authSessionService);
  }

  @Test
  @DisplayName("탈퇴한 사용자는 정지할 수 없고 탈퇴 상태로 남는다.")
  void banUser_RejectsDeletedUser() {
    User deleted = User.builder().id(1L).nickname("deleted").status(UserStatus.DELETED).build();
    given(userRepository.findById(1L)).willReturn(Optional.of(deleted));

    assertThatThrownBy(() -> userService.banUser(99L, 1L))
        .isInstanceOfSatisfying(
            UserException.class, e -> assertThat(e.getErrorSpec()).isEqualTo(UserError.DELETED));
    assertThat(deleted.getStatus()).isEqualTo(UserStatus.DELETED);
    verifyNoInteractions(authSessionService);
  }

  @Test
  @DisplayName("정지를 해제하면 정상 상태로 돌아온다.")
  void unbanUser_Success() {
    User banned = User.builder().id(1L).nickname("banned").status(UserStatus.BANNED).build();
    given(userRepository.findById(1L)).willReturn(Optional.of(banned));

    userService.unbanUser(99L, 1L);

    assertThat(banned.getStatus()).isEqualTo(UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("정지 상태가 아닌 사용자는 해제해도 상태가 바뀌지 않는다. 탈퇴한 사용자가 되살아나지 않는다.")
  void unbanUser_LeavesNonBannedUserAsIs() {
    User deleted = User.builder().id(1L).nickname("deleted").status(UserStatus.DELETED).build();
    given(userRepository.findById(1L)).willReturn(Optional.of(deleted));

    userService.unbanUser(99L, 1L);

    assertThat(deleted.getStatus()).isEqualTo(UserStatus.DELETED);
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
