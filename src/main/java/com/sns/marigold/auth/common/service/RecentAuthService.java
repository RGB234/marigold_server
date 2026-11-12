package com.sns.marigold.auth.common.service;

import java.time.Instant;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.sns.marigold.auth.common.AccessTokenDetails;
import com.sns.marigold.auth.common.recent.RecentAuthProperties;
import com.sns.marigold.auth.common.session.AuthSessionStore;
import com.sns.marigold.auth.exception.AuthException;

import lombok.RequiredArgsConstructor;

/** 민감한 작업은 그 요청의 세션이 최근(기본 5분 안)에 로그인으로 만들어졌을 때만 허용합니다. 재인증은 다시 로그인하는 것입니다. */
@Service
@RequiredArgsConstructor
public class RecentAuthService {

  private final AuthSessionStore sessionStore;
  private final RecentAuthProperties recentAuthProperties;

  /** access token으로 인증된 요청의 세션을 확인합니다. */
  public void validate(@Nullable Authentication authentication) {
    if (authentication == null
        || !(authentication.getDetails() instanceof AccessTokenDetails details)) {
      throw AuthException.forRecentAuthRequired();
    }
    validate(details.sessionId());
  }

  public void validate(String sessionId) {
    Instant authenticatedAt = sessionStore.getAuthenticatedAt(sessionId);
    if (authenticatedAt == null
        || !authenticatedAt.plusSeconds(recentAuthProperties.ttlSeconds()).isAfter(Instant.now())) {
      throw AuthException.forRecentAuthRequired();
    }
  }
}
