package com.sns.marigold.auth.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.dto.LocalLoginDto;
import com.sns.marigold.auth.common.dto.LoginResponseDto;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.service.AuthService;
import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.auth.common.service.AuthSessionService.RotatedSession;
import com.sns.marigold.auth.common.session.SessionTokens;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.auth.oauth2.RandomUsernameGenerator;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.dto.create.LocalSignupDto;
import com.sns.marigold.user.dto.create.OAuth2SignupDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.enums.UserStatus;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private AuthSessionService authSessionService;

  @Mock private CookieManager cookieManager;

  @Mock private RandomUsernameGenerator randomUsernameGenerator;

  @Mock private AuditLogger auditLogger;

  private AuthService authService;
  private SimpleMeterRegistry meterRegistry;

  private User testUser;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    authService =
        new AuthService(
            userRepository,
            passwordEncoder,
            authSessionService,
            cookieManager,
            randomUsernameGenerator,
            auditLogger,
            meterRegistry);

    testUser =
        User.builder()
            .id(1L)
            .email("test@example.com")
            .password("encodedPassword")
            .nickname("tester")
            .role(Role.ROLE_PERSON)
            .status(UserStatus.ACTIVE)
            .build();
  }

  private LocalSignupDto createLocalSignupDto(String email, String password, String nickname) {
    LocalSignupDto dto = new LocalSignupDto();
    ReflectionTestUtils.setField(dto, "email", email);
    ReflectionTestUtils.setField(dto, "password", password);
    ReflectionTestUtils.setField(dto, "nickname", nickname);
    return dto;
  }

  private LocalLoginDto createEmailLoginDto(String email, String password) {
    LocalLoginDto dto = new LocalLoginDto();
    ReflectionTestUtils.setField(dto, "email", email);
    ReflectionTestUtils.setField(dto, "password", password);
    return dto;
  }

  @Test
  @DisplayName("이메일 회원가입 시 정상적으로 사용자를 저장한다.")
  void localSignup_Success() {
    // given
    LocalSignupDto dto = createLocalSignupDto("new@example.com", "password123", "newbie");

    given(userRepository.existsByEmail(dto.getEmail())).willReturn(false);
    given(userRepository.existsByNickname(dto.getNickname())).willReturn(false);
    given(passwordEncoder.encode(dto.getPassword())).willReturn("encodedPassword123");

    // when
    authService.localSignup(dto);

    // then
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("이메일 회원가입 시 이메일이 중복되면 예외가 발생한다.")
  void localSignup_EmailAlreadyExists() {
    // given
    LocalSignupDto dto = createLocalSignupDto("test@example.com", "password123", "newbie");

    given(userRepository.existsByEmail(dto.getEmail())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> authService.localSignup(dto))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserAlreadyExists().getMessage());
  }

  @Test
  @DisplayName("이메일 회원가입 시 닉네임이 중복되면 예외가 발생한다.")
  void localSignup_NicknameAlreadyExists() {
    // given
    LocalSignupDto dto = createLocalSignupDto("new@example.com", "password123", "tester");

    given(userRepository.existsByEmail(dto.getEmail())).willReturn(false);
    given(userRepository.existsByNickname(dto.getNickname())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> authService.localSignup(dto))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserNicknameAlreadyExists().getMessage());
  }

  @Test
  @DisplayName("OAuth2 회원가입 시 고유한 닉네임을 생성하고 사용자를 저장한다.")
  void oauth2Signup_Success() {
    // given
    OAuth2SignupDto dto =
        OAuth2SignupDto.builder()
            .providerInfo(ProviderInfo.KAKAO)
            .providerId("12345")
            .role(Role.ROLE_PERSON)
            .build();

    given(userRepository.existsByProviderInfoAndProviderId(ProviderInfo.KAKAO, "12345"))
        .willReturn(false);
    given(randomUsernameGenerator.generate()).willReturn("generatedNick");
    given(userRepository.existsByNickname("generatedNick")).willReturn(false);

    // when
    authService.oauth2Signup(dto);

    // then
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("OAuth2 회원가입 시 이미 존재하는 회원이면 예외가 발생한다.")
  void oauth2Signup_AlreadyExists() {
    // given
    OAuth2SignupDto dto =
        OAuth2SignupDto.builder()
            .providerInfo(ProviderInfo.KAKAO)
            .providerId("12345")
            .role(Role.ROLE_PERSON)
            .build();

    given(userRepository.existsByProviderInfoAndProviderId(ProviderInfo.KAKAO, "12345"))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> authService.oauth2Signup(dto))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserAlreadyExists().getMessage());
  }

  @Test
  @DisplayName("이메일 로그인 성공 시 토큰을 생성하고 쿠키에 추가한다.")
  void emailLogin_Success() {
    // given
    LocalLoginDto dto = createEmailLoginDto("test@example.com", "password123");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(testUser));
    given(passwordEncoder.matches(dto.getPassword(), testUser.getPassword())).willReturn(true);
    given(authSessionService.start(any(CustomPrincipal.class)))
        .willReturn(new SessionTokens("access_token_value", "refresh_token_value", 86400L));

    // when
    LoginResponseDto result = authService.emailLogin(dto, request, response);

    // then
    verify(cookieManager, times(1))
        .addCookie(
            eq(response),
            eq(CookieManager.REFRESH_TOKEN_NAME),
            eq("refresh_token_value"),
            eq(86400L));
    verify(authSessionService, never()).endPrevious(any());
    assertThat(result.getAccessToken()).isEqualTo("access_token_value");
    assertThat(result.getPrincipal().getUserId()).isEqualTo(testUser.getId());
    assertThat(result.getPrincipal().getAuthorities()).containsExactly(Role.ROLE_PERSON.name());
    assertThat(meterRegistry.get("auth.login.find_by_email").timer().count()).isEqualTo(1);
    assertThat(meterRegistry.get("auth.login.password_matches").timer().count()).isEqualTo(1);
    assertThat(meterRegistry.get("auth.login.jwt_create").timer().count()).isEqualTo(1);
  }

  @Test
  @DisplayName("refresh 쿠키가 있는 채로 다시 로그인하면 새 세션을 만든 뒤 이전 세션을 폐기한다.")
  void emailLogin_EndsPreviousSession() {
    LocalLoginDto dto = createEmailLoginDto("test@example.com", "password123");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(testUser));
    given(passwordEncoder.matches(dto.getPassword(), testUser.getPassword())).willReturn(true);
    given(cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME))
        .willReturn(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "previous_refresh_token"));
    given(authSessionService.start(any(CustomPrincipal.class)))
        .willReturn(new SessionTokens("access_token_value", "refresh_token_value", 86400L));

    authService.emailLogin(dto, request, response);

    InOrder inOrder = inOrder(authSessionService);
    inOrder.verify(authSessionService).start(any(CustomPrincipal.class));
    inOrder.verify(authSessionService).endPrevious("previous_refresh_token");
    verify(cookieManager)
        .addCookie(response, CookieManager.REFRESH_TOKEN_NAME, "refresh_token_value", 86400L);
  }

  @Test
  @DisplayName("토큰 재발급 시 세션에서 찾은 사용자의 현재 권한으로 access token을 발급한다.")
  void reissue_ReturnsCompleteSession() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    User latestUser =
        User.builder()
            .id(testUser.getId())
            .email(testUser.getEmail())
            .password(testUser.getPassword())
            .nickname(testUser.getNickname())
            .role(Role.ROLE_ADMIN)
            .status(UserStatus.ACTIVE)
            .build();

    given(cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME))
        .willReturn(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh_token_value"));
    given(authSessionService.rotate("refresh_token_value"))
        .willReturn(new RotatedSession(testUser.getId(), "session-1", "new_refresh_token", 3600L));
    given(userRepository.findById(testUser.getId())).willReturn(Optional.of(latestUser));
    given(authSessionService.createAccessToken(any(CustomPrincipal.class), eq("session-1")))
        .willReturn("new_access_token");

    LoginResponseDto result = authService.reissue(request, response);

    assertThat(result.getAccessToken()).isEqualTo("new_access_token");
    assertThat(result.getPrincipal().getUserId()).isEqualTo(testUser.getId());
    assertThat(result.getPrincipal().getAuthorities()).containsExactly(Role.ROLE_ADMIN.name());
    verify(cookieManager)
        .addCookie(response, CookieManager.REFRESH_TOKEN_NAME, "new_refresh_token", 3600L);
  }

  @Test
  @DisplayName("refresh 쿠키가 없으면 재발급할 수 없다.")
  void reissue_RejectsMissingCookie() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    assertThatThrownBy(() -> authService.reissue(request, response))
        .isInstanceOf(AuthException.class);
    verifyNoInteractions(authSessionService);
  }

  @Test
  @DisplayName("삭제된 사용자는 토큰을 재발급할 수 없고, 방금 회전한 세션도 폐기한다.")
  void reissue_RejectsDeletedUser() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    User deletedUser =
        User.builder()
            .id(testUser.getId())
            .email(testUser.getEmail())
            .password(testUser.getPassword())
            .nickname(testUser.getNickname())
            .role(Role.ROLE_PERSON)
            .status(UserStatus.DELETED)
            .build();

    given(cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME))
        .willReturn(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh_token_value"));
    given(authSessionService.rotate("refresh_token_value"))
        .willReturn(new RotatedSession(testUser.getId(), "session-1", "new_refresh_token", 3600L));
    given(userRepository.findById(testUser.getId())).willReturn(Optional.of(deletedUser));

    assertThatThrownBy(() -> authService.reissue(request, response))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserDeleted().getMessage());
    verify(authSessionService).revoke("session-1");
    verify(cookieManager, never()).addCookie(any(), any(), any(), anyLong());
  }

  @Test
  @DisplayName("이메일 로그인 시 비밀번호가 틀리면 예외가 발생한다.")
  void emailLogin_InvalidPassword() {
    // given
    LocalLoginDto dto = createEmailLoginDto("test@example.com", "wrongpassword");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(testUser));
    given(passwordEncoder.matches(dto.getPassword(), testUser.getPassword())).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.emailLogin(dto, request, response))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forInvalidCredentials().getMessage());
  }

  @Test
  @DisplayName("로그인 시도 유저가 탈퇴 상태라면 예외가 발생한다.")
  void emailLogin_DeletedUser() {
    // given
    LocalLoginDto dto = createEmailLoginDto("test@example.com", "password123");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    User deletedUser =
        User.builder()
            .id(2L)
            .email("test@example.com")
            .password("encodedPassword")
            .status(UserStatus.DELETED)
            .build();

    given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(deletedUser));
    given(passwordEncoder.matches(dto.getPassword(), deletedUser.getPassword())).willReturn(true);

    // when & then
    assertThatThrownBy(() -> authService.emailLogin(dto, request, response))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserDeleted().getMessage());
  }

  @Test
  @DisplayName("인증 정보가 유효할 경우 사용자의 인증 상태 정보를 반환한다.")
  void getAuthStatus_Success() {
    // given
    CustomPrincipal principal =
        new CustomPrincipal(
            1L,
            List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name())),
            null,
            AuthStatus.LOGIN_SUCCESS);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    HttpServletRequest request = mock(HttpServletRequest.class);

    // when
    UserAuthStatusDto statusDto = authService.getAuthStatus(authentication, request);

    // then
    assertThat(statusDto.getUserId()).isEqualTo(1L);
    assertThat(statusDto.getAuthorities()).hasSize(1);
    assertThat(statusDto.getAuthorities().get(0)).isEqualTo(Role.ROLE_PERSON.name());
    assertThat(statusDto.isRefreshTokenPresent()).isFalse();
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("unauthenticatedUsers")
  @DisplayName("인증 정보가 없거나 익명 사용자이면 빈 상태 정보를 반환한다.")
  void getAuthStatus_Unauthenticated(Authentication authentication) {
    HttpServletRequest request = mock(HttpServletRequest.class);

    // when
    UserAuthStatusDto statusDto = authService.getAuthStatus(authentication, request);

    // then
    assertThat(statusDto.getUserId()).isNull();
    assertThat(statusDto.getAuthorities()).isEmpty();
    assertThat(statusDto.isRefreshTokenPresent()).isFalse();
  }

  private static Stream<Authentication> unauthenticatedUsers() {
    return Stream.of(
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
  }

  @Test
  @DisplayName("Refresh Token 쿠키가 있으면 세션 복구 가능 상태를 반환한다.")
  void getAuthStatus_RefreshTokenPresent() {
    // given
    HttpServletRequest request = mock(HttpServletRequest.class);
    given(cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME))
        .willReturn(new Cookie(CookieManager.REFRESH_TOKEN_NAME, "refresh-token"));

    // when
    UserAuthStatusDto statusDto = authService.getAuthStatus(null, request);

    // then
    assertThat(statusDto.isRefreshTokenPresent()).isTrue();
  }
}
