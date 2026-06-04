package com.sns.marigold.auth.common.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

  private final JwtManager jwtManager;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public Authentication getAuthentication(String accessToken) {
    Claims claims = jwtManager.getClaims(accessToken);
    Long userId = jwtManager.getUserId(claims);
    List<SimpleGrantedAuthority> authorities = jwtManager.getAuthorities(claims);

    if (authorities.isEmpty()) {
      throw AuthException.forInvalidToken();
    }

    User user =
        userRepository
            .findById(Objects.requireNonNull(userId))
            .orElseThrow(() -> UserException.forUserNotFound());
    checkUserStatus(user);

    CustomPrincipal principal =
        new CustomPrincipal(user.getId(), authorities, null, AuthStatus.LOGIN_SUCCESS);

    return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
  }

  private void checkUserStatus(User user) {
    switch (user.getStatus()) {
      case DELETED -> throw UserException.forUserDeleted();
      case BANNED -> throw UserException.forUserBanned();
      case SLEEP -> throw UserException.forUserSleeping();
      default -> {}
    }
  }
}
