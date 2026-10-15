package com.sns.marigold.auth.common.service;

import java.util.List;
import java.util.Objects;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.exception.AuthException;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtAuthenticationService {

  private final JwtManager jwtManager;

  public Authentication getAuthentication(String accessToken) {
    Claims claims = jwtManager.getClaims(accessToken);

    if (!jwtManager.isAccessToken(claims)) {
      throw AuthException.forInvalidToken();
    }

    Long userId = jwtManager.getUserId(claims);
    List<SimpleGrantedAuthority> authorities = jwtManager.getAuthorities(claims);

    if (authorities.isEmpty()) {
      throw AuthException.forInvalidToken();
    }

    CustomPrincipal principal =
        new CustomPrincipal(
            Objects.requireNonNull(userId), authorities, null, AuthStatus.LOGIN_SUCCESS);

    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
    authentication.setDetails(Objects.requireNonNull(claims.getExpiration()).toInstant());
    return authentication;
  }
}
