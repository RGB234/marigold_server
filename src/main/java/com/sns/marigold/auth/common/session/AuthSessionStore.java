package com.sns.marigold.auth.common.session;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.lang.Nullable;

/** 로그인 세션별로 현재 refresh token 비밀값의 해시와 만료 시각을 보관합니다. */
public interface AuthSessionStore {

  /**
   * @param tokenHash refresh token 비밀값의 해시({@link RefreshToken#secretHash()})
   * @param authenticatedAt 로그인한 시각. 최근 인증 판단에 쓰고 refresh로 바뀌지 않는다
   */
  void create(
      String sessionId,
      Long userId,
      String tokenHash,
      Instant authenticatedAt,
      Instant expiresAt,
      Instant absoluteExpiresAt);

  /**
   * 제시된 refresh token이 현재 token이면(또는 유예 시간 안의 직전 token이면) 새 token으로 바꿉니다. 그 밖의 token이 오면 탈취로 보고 세션을
   * 지웁니다.
   */
  Rotation rotate(
      String sessionId,
      String presentedTokenHash,
      String newTokenHash,
      Instant now,
      Duration refreshTtl,
      Duration reuseGrace);

  boolean exists(String sessionId);

  /** 세션이 없으면 null을 돌려줍니다. */
  @Nullable
  Instant getAuthenticatedAt(String sessionId);

  /** 제시된 해시가 세션의 현재 refresh token이면 사용자 ID를, 아니거나 세션이 없으면 null을 돌려줍니다. */
  @Nullable
  Long findUserId(String sessionId, String tokenHash);

  void delete(String sessionId);

  /** 사용자의 모든 세션을 지우고 지운 세션 ID를 돌려줍니다. */
  List<String> deleteAll(Long userId);

  /**
   * @param userId {@link Status#MISSING}이 아니면 세션의 사용자
   * @param expiresAt {@link Status#ROTATED}일 때 세션의 새 만료 시각
   */
  record Rotation(Status status, @Nullable Long userId, @Nullable Instant expiresAt) {

    public enum Status {
      ROTATED,
      /** 세션이 없음: 로그아웃했거나 만료되었거나 이미 폐기됨 */
      MISSING,
      /** 현재 token이 아닌 token이 와서 세션을 폐기함 */
      REUSED
    }
  }
}
