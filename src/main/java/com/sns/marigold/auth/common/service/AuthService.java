package com.sns.marigold.auth.common.service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.dto.LocalLoginDto;
import com.sns.marigold.auth.common.dto.UserAuthStatusDto;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.util.CookieManager;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.user.dto.create.LocalSignupDto;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtManager jwtManager;
  private final CookieManager cookieManager;

  // OAuth2 로그인/로그아웃 & 회원가입 -> Spring security 에서 처리 (SecurityConfig & OAuth2UserService)

  public UserAuthStatusDto getAuthStatus(Authentication authentication) {
    // JwtAuthenticationFilter에서 Authentication 객체 생성 후 SecurityContext에 저장함
    // Spring Security에서 'anonymousUser'는 String 타입이므로 instanceof 체크 필수
    if (authentication == null || !(authentication.getPrincipal() instanceof CustomPrincipal)) {

      return new UserAuthStatusDto(null, Collections.emptyList());
    }

    // 안전하게 캐스팅
    CustomPrincipal userPrincipal = (CustomPrincipal) authentication.getPrincipal();

    // DB 조회 없이 토큰(Principal)에 있는 정보로만 응답
    // JWT 필터를 통과했다면 이미 검증된 사용자라고 신뢰함.
    // 보안상 민감한 부분에서는 추후 DB에서 사용자 정보를 조회하여 검증하도록 함.
    return new UserAuthStatusDto(
        userPrincipal.getUserId(), userPrincipal.getAuthorities().stream().toList());
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

    userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public void localLogin(LocalLoginDto dto, HttpServletResponse response) {
    User user =
        userRepository
            .findByEmail(dto.getEmail())
            .orElseThrow(() -> UserException.forUserNotFound());

    if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
      throw AuthException.forInvalidCredentials(); // 비밀번호 불일치
    }

    CustomPrincipal principal =
        new CustomPrincipal(
            user.getId(), List.of(new SimpleGrantedAuthority(user.getRole().name())), null);

    String accessToken = jwtManager.createAccessToken(principal);
    String refreshToken = jwtManager.createRefreshToken(principal);

    cookieManager.addCookie(
        response,
        cookieManager.ACCESS_TOKEN_NAME,
        accessToken,
        jwtManager.getAccessTokenValidityInSeconds());
    cookieManager.addCookie(
        response,
        cookieManager.REFRESH_TOKEN_NAME,
        refreshToken,
        jwtManager.getRefreshTokenValidityInSeconds());
  }
}
