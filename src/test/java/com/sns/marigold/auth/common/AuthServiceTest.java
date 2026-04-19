package com.sns.marigold.auth.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sns.marigold.auth.common.dto.LocalLoginDto;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.service.AuthService;
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
import io.hypersistence.tsid.TSID;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtManager jwtManager;

  @Mock private CookieManager cookieManager;

  @Mock private RandomUsernameGenerator randomUsernameGenerator;

  @InjectMocks private AuthService authService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(TSID.from(1L).toLong())
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

  private LocalLoginDto createLocalLoginDto(String email, String password) {
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
  void localLogin_Success() {
    // given
    LocalLoginDto dto = createLocalLoginDto("test@example.com", "password123");
    HttpServletResponse response = mock(HttpServletResponse.class);

    given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(testUser));
    given(passwordEncoder.matches(dto.getPassword(), testUser.getPassword())).willReturn(true);
    given(jwtManager.createAccessToken(any(CustomPrincipal.class)))
        .willReturn("access_token_value");
    given(jwtManager.createRefreshToken(any(CustomPrincipal.class)))
        .willReturn("refresh_token_value");
    given(jwtManager.getRefreshTokenValidityInSeconds()).willReturn(86400L);

    // when
    authService.localLogin(dto, response);

    // then
    verify(cookieManager, times(1))
        .addCookie(
            eq(response),
            eq(CookieManager.REFRESH_TOKEN_NAME),
            eq("refresh_token_value"),
            eq(86400L));
  }

  @Test
  @DisplayName("이메일 로그인 시 비밀번호가 틀리면 예외가 발생한다.")
  void localLogin_InvalidPassword() {
    // given
    LocalLoginDto dto = createLocalLoginDto("test@example.com", "wrongpassword");
    HttpServletResponse response = mock(HttpServletResponse.class);

    given(userRepository.findByEmail(dto.getEmail())).willReturn(Optional.of(testUser));
    given(passwordEncoder.matches(dto.getPassword(), testUser.getPassword())).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.localLogin(dto, response))
        .isInstanceOf(AuthException.class)
        .hasMessageContaining(AuthException.forInvalidCredentials().getMessage());
  }

  @Test
  @DisplayName("로그인 시도 유저가 탈퇴 상태라면 예외가 발생한다.")
  void localLogin_DeletedUser() {
    // given
    LocalLoginDto dto = createLocalLoginDto("test@example.com", "password123");
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
    assertThatThrownBy(() -> authService.localLogin(dto, response))
        .isInstanceOf(UserException.class)
        .hasMessageContaining(UserException.forUserDeleted().getMessage());
  }

  @Test
  @DisplayName("인증 정보가 유효할 경우 사용자의 인증 상태 정보를 반환한다.")
  void getAuthStatus_Success() {
    // given
    CustomPrincipal principal =
        new CustomPrincipal(
            TSID.from(1L).toLong(),
            List.of(new SimpleGrantedAuthority(Role.ROLE_PERSON.name())),
            null,
            AuthStatus.LOGIN_SUCCESS);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

    // when
    UserAuthStatusDto statusDto = authService.getAuthStatus(authentication);

    // then
    assertThat(statusDto.getUserId()).isEqualTo(TSID.from(1L).toString());
    assertThat(statusDto.getAuthorities()).hasSize(1);
    assertThat(statusDto.getAuthorities().get(0)).isEqualTo(Role.ROLE_PERSON.name());
  }

  @Test
  @DisplayName("인증 정보가 익명 사용자일 경우 빈 상태 정보를 반환한다.")
  void getAuthStatus_AnonymousUser() {
    // given
    Authentication authentication =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

    // when
    UserAuthStatusDto statusDto = authService.getAuthStatus(authentication);

    // then
    assertThat(statusDto.getUserId()).isNull();
    assertThat(statusDto.getAuthorities()).isEmpty();
  }

  @Test
  @DisplayName("인증 정보 자체가 null일 경우 빈 상태 정보를 반환한다.")
  void getAuthStatus_NullAuthentication() {
    // when
    UserAuthStatusDto statusDto = authService.getAuthStatus(null);

    // then
    assertThat(statusDto.getUserId()).isNull();
    assertThat(statusDto.getAuthorities()).isEmpty();
  }
}
