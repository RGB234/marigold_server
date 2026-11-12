package com.sns.marigold.auth.common.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.dto.AuthPrincipalDto;
import com.sns.marigold.auth.common.dto.LocalLoginDto;
import com.sns.marigold.auth.common.dto.LoginResponseDto;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.service.AuthSessionService.RotatedSession;
import com.sns.marigold.auth.common.session.SessionTokens;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.auth.oauth2.RandomUsernameGenerator;
import com.sns.marigold.user.dto.create.LocalSignupDto;
import com.sns.marigold.user.dto.create.OAuth2SignupDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private static final double[] AUTH_LOGIN_PERCENTILES = {0.5, 0.95, 0.99};
  private static final String AUTH_LOGIN_FIND_BY_EMAIL = "auth.login.find_by_email";
  private static final String AUTH_LOGIN_PASSWORD_MATCHES = "auth.login.password_matches";
  private static final String AUTH_LOGIN_JWT_CREATE = "auth.login.jwt_create";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthSessionService authSessionService;
  private final CookieManager cookieManager;
  private final RandomUsernameGenerator randomUsernameGenerator;
  private final AuditLogger auditLogger;
  private final MeterRegistry meterRegistry;

  private Timer authLoginFindByEmailTimer;
  private Timer authLoginPasswordMatchesTimer;
  private Timer authLoginJwtCreateTimer;

  // OAuth2 로그인/로그아웃 & 회원가입 -> Spring security 에서 처리 (SecurityConfig &
  // OAuth2UserService)

  public UserAuthStatusDto getAuthStatus(
      Authentication authentication, HttpServletRequest request) {
    boolean refreshTokenPresent =
        cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME) != null;

    // JwtAuthenticationFilter에서 Authentication 객체 생성 후 SecurityContext에 저장함
    // Spring Security에서 'anonymousUser'는 String 타입이므로 instanceof 체크 필수
    if (authentication == null || !(authentication.getPrincipal() instanceof CustomPrincipal)) {

      return new UserAuthStatusDto(null, Collections.emptyList(), refreshTokenPresent);
    }

    // 안전하게 캐스팅
    CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();

    // DB 조회 없이 토큰(Principal)에 있는 정보로만 응답
    // JWT 필터를 통과했다면 이미 검증된 사용자라고 신뢰함.
    // 보안상 민감한 부분에서는 추후 DB에서 사용자 정보를 조회하여 검증하도록 함.
    return new UserAuthStatusDto(
        userPrincipal.getUserId(),
        userPrincipal.getAuthorities().stream().toList(),
        refreshTokenPresent);
  }

  @Transactional
  public void localSignup(LocalSignupDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
      throw UserException.forUserAlreadyExists();
    }
    if (userRepository.existsByNickname(dto.getNickname())) {
      throw UserException.forUserNicknameAlreadyExists();
    }

    User user =
        User.builder()
            .email(dto.getEmail())
            .password(passwordEncoder.encode(dto.getPassword()))
            .nickname(dto.getNickname())
            .build();

    userRepository.save(Objects.requireNonNull(user));
    auditLogger.info("event=local_signup_success userId={}", user.getId());
  }

  @Transactional
  public Long oauth2Signup(OAuth2SignupDto dto) {
    Objects.requireNonNull(dto, "OAuth2SignupDto는 null일 수 없습니다.");

    if (userRepository.existsByProviderInfoAndProviderId(
        dto.getProviderInfo(), dto.getProviderId())) {
      throw UserException.forUserAlreadyExists();
    }

    // nickname 자동 생성
    String generatedNickname = generateUniqueNickname();

    // User 엔티티 생성
    User user =
        User.builder()
            .providerInfo(dto.getProviderInfo())
            .providerId(dto.getProviderId())
            .nickname(generatedNickname)
            .role(dto.getRole())
            .build();

    userRepository.save(Objects.requireNonNull(user));
    auditLogger.info(
        "event=oauth2_signup_success userId={} provider={}", user.getId(), dto.getProviderInfo());

    // 저장
    return user.getId();
  }

  /** 고유한 nickname 생성 (중복 체크 포함) */
  private String generateUniqueNickname() {
    int maxAttempts = 10; // 최대 시도 횟수
    String nickname;

    for (int i = 0; i < maxAttempts; i++) {
      nickname = randomUsernameGenerator.generate();

      // 중복 체크
      if (!userRepository.existsByNickname(nickname)) {
        return nickname;
      }

      log.debug("Nickname duplicated. Retrying generation.");
    }

    // 최대 시도 횟수 초과 시 예외 발생
    throw UserException.forUserNicknameAlreadyExists();
  }

  @Transactional(readOnly = true)
  public LoginResponseDto emailLogin(
      LocalLoginDto dto, HttpServletRequest request, HttpServletResponse response) {
    User user =
        authLoginFindByEmailTimer()
            .record(() -> userRepository.findByEmail(dto.getEmail()))
            .orElseThrow(() -> UserException.forUserNotFound());

    if (!authLoginPasswordMatchesTimer()
        .record(() -> passwordEncoder.matches(dto.getPassword(), user.getPassword()))) {
      throw AuthException.forInvalidCredentials(); // 비밀번호 불일치
    }

    checkUserStatus(user);

    CustomPrincipal principal =
        new CustomPrincipal(
            user.getId(),
            List.of(new SimpleGrantedAuthority(user.getRole().name())),
            null,
            AuthStatus.LOGIN_SUCCESS);

    SessionTokens tokens =
        Objects.requireNonNull(
            authLoginJwtCreateTimer().record(() -> authSessionService.start(principal)));

    // 새 쿠키가 이전 쿠키를 덮어쓰면 이전 세션은 아무도 쓰지 않은 채 만료까지 남으므로 지금 폐기한다
    Cookie previous = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);
    if (previous != null) {
      authSessionService.endPrevious(previous.getValue());
    }

    cookieManager.addCookie(
        response,
        CookieManager.REFRESH_TOKEN_NAME,
        tokens.refreshToken(),
        tokens.refreshMaxAgeSeconds());

    return createLoginResponse(tokens.accessToken(), principal);
  }

  @Transactional(readOnly = true)
  public LoginResponseDto reissue(HttpServletRequest request, HttpServletResponse response) {
    Cookie cookie = cookieManager.getCookie(request, CookieManager.REFRESH_TOKEN_NAME);
    if (cookie == null) {
      throw AuthException.forInvalidToken();
    }
    RotatedSession rotated = authSessionService.rotate(cookie.getValue());

    // 사용자 ID는 세션에서 나오므로 회전한 뒤에 DB 상태를 본다. 거부할 때는 회전된 세션도 지워,
    // 쿠키를 받지 못한 다음 시도가 재사용 감지로 기록되지 않게 한다
    User user;
    try {
      user =
          userRepository
              .findById(Objects.requireNonNull(rotated.userId()))
              .orElseThrow(() -> UserException.forUserNotFound());
      checkUserStatus(user);
    } catch (UserException e) {
      authSessionService.revoke(rotated.sessionId());
      throw e;
    }

    CustomPrincipal principal =
        new CustomPrincipal(
            user.getId(),
            List.of(new SimpleGrantedAuthority(user.getRole().name())),
            null,
            AuthStatus.LOGIN_SUCCESS);

    cookieManager.addCookie(
        response,
        CookieManager.REFRESH_TOKEN_NAME,
        rotated.refreshToken(),
        rotated.refreshMaxAgeSeconds());

    return createLoginResponse(
        authSessionService.createAccessToken(principal, rotated.sessionId()), principal);
  }

  private LoginResponseDto createLoginResponse(String accessToken, CustomPrincipal principal) {
    AuthPrincipalDto principalDto =
        new AuthPrincipalDto(
            principal.getUserId(),
            principal.getAuthorities().stream().map(SimpleGrantedAuthority::getAuthority).toList());
    return new LoginResponseDto(accessToken, principalDto);
  }

  public void checkUserStatus(User user) {
    switch (user.getStatus()) {
      case DELETED -> throw UserException.forUserDeleted();
      case BANNED -> throw UserException.forUserBanned();
      default -> {}
    }
  }

  private Timer authLoginFindByEmailTimer() {
    if (authLoginFindByEmailTimer == null) {
      authLoginFindByEmailTimer =
          authLoginTimer(
              AUTH_LOGIN_FIND_BY_EMAIL, "Time spent finding a user by email during login");
    }
    return authLoginFindByEmailTimer;
  }

  private Timer authLoginPasswordMatchesTimer() {
    if (authLoginPasswordMatchesTimer == null) {
      authLoginPasswordMatchesTimer =
          authLoginTimer(
              AUTH_LOGIN_PASSWORD_MATCHES, "Time spent verifying the login password hash");
    }
    return authLoginPasswordMatchesTimer;
  }

  private Timer authLoginJwtCreateTimer() {
    if (authLoginJwtCreateTimer == null) {
      authLoginJwtCreateTimer =
          authLoginTimer(AUTH_LOGIN_JWT_CREATE, "Time spent creating the login session and JWTs");
    }
    return authLoginJwtCreateTimer;
  }

  private Timer authLoginTimer(String name, String description) {
    return Timer.builder(name)
        .description(description)
        .publishPercentiles(AUTH_LOGIN_PERCENTILES)
        .publishPercentileHistogram()
        .register(meterRegistry);
  }
}
