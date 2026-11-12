package com.sns.marigold.auth.common.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.sns.marigold.auth.exception.AuthException;

import lombok.RequiredArgsConstructor;

/**
 * 세션은 {@code auth:session:{sid}} HASH 하나에 두고, 사용자별 전체 로그아웃을 위해 {@code auth:user:{userId}:sessions}
 * ZSET(score = 절대 만료 시각)에 sid를 모읍니다. 각 스크립트는 키 하나만 다뤄 Redis Cluster에서도 같은 slot 제약에 걸리지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class RedisAuthSessionStore implements AuthSessionStore {

  private static final String SESSION_KEY_PREFIX = "auth:session:";
  private static final String USER_SESSIONS_KEY_FORMAT = "auth:user:%d:sessions";

  private static final RedisScript<Long> CREATE_SCRIPT =
      RedisScript.of(
          """
          redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'tokenHash', ARGV[2], 'authenticatedAt', ARGV[3], 'absoluteExpiresAt', ARGV[5])
          redis.call('PEXPIREAT', KEYS[1], ARGV[4])
          return 1
          """,
          Long.class);

  // 반환값: {상태, 사용자 ID}. 상태는 새 만료 시각(epoch ms), 0 = 세션 없음, -1 = 재사용 감지로 세션 폐기
  // ARGV: 1 제시된 token 해시, 2 새 token 해시, 3 현재 시각 ms, 4 refresh TTL ms, 5 재사용 유예 ms
  @SuppressWarnings("rawtypes")
  private static final RedisScript<List> ROTATE_SCRIPT =
      RedisScript.of(
          """
          local current = redis.call('HGET', KEYS[1], 'tokenHash')
          if not current then
            return {0}
          end
          local userId = redis.call('HGET', KEYS[1], 'userId')
          local now = tonumber(ARGV[3])
          if current == ARGV[1] then
            redis.call('HSET', KEYS[1], 'previousTokenHash', current, 'rotatedAt', ARGV[3])
          else
            -- 직전 token의 재시도는 유예 안에서만 받아 주고, 직전 token과 회전 시각은 그대로 둬 유예가 늘어나지 않게 한다
            local previous = redis.call('HGET', KEYS[1], 'previousTokenHash')
            local rotatedAt = tonumber(redis.call('HGET', KEYS[1], 'rotatedAt'))
            if previous ~= ARGV[1] or not rotatedAt or now - rotatedAt > tonumber(ARGV[5]) then
              redis.call('DEL', KEYS[1])
              return {-1, userId}
            end
          end
          redis.call('HSET', KEYS[1], 'tokenHash', ARGV[2])
          local absoluteExpiresAt = tonumber(redis.call('HGET', KEYS[1], 'absoluteExpiresAt'))
          local expiresAt = math.min(now + tonumber(ARGV[4]), absoluteExpiresAt)
          redis.call('PEXPIREAT', KEYS[1], expiresAt)
          return {expiresAt, userId}
          """,
          List.class);

  private final StringRedisTemplate redisTemplate;

  @Override
  public void create(
      String sessionId,
      Long userId,
      String tokenHash,
      Instant authenticatedAt,
      Instant expiresAt,
      Instant absoluteExpiresAt) {
    long absoluteExpiresAtMillis = absoluteExpiresAt.toEpochMilli();
    call(
        () -> {
          // 색인을 먼저 써서, 세션 생성이 실패해도 색인에 남는 것은 지워도 무방한 sid뿐이게 한다
          String userSessionsKey = userSessionsKey(userId);
          redisTemplate.opsForZSet().add(userSessionsKey, sessionId, absoluteExpiresAtMillis);
          redisTemplate
              .opsForZSet()
              .removeRangeByScore(
                  userSessionsKey, Double.NEGATIVE_INFINITY, Instant.now().toEpochMilli());
          // 절대 만료는 세션마다 같은 길이라, 가장 최근 세션의 절대 만료까지 두면 모든 세션보다 오래 남는다
          redisTemplate.expireAt(userSessionsKey, absoluteExpiresAt);
          return redisTemplate.execute(
              CREATE_SCRIPT,
              List.of(sessionKey(sessionId)),
              userId.toString(),
              tokenHash,
              String.valueOf(authenticatedAt.toEpochMilli()),
              String.valueOf(expiresAt.toEpochMilli()),
              String.valueOf(absoluteExpiresAtMillis));
        });
  }

  @Override
  public Rotation rotate(
      String sessionId,
      String presentedTokenHash,
      String newTokenHash,
      Instant now,
      Duration refreshTtl,
      Duration reuseGrace) {
    List<?> result =
        call(
            () ->
                redisTemplate.execute(
                    ROTATE_SCRIPT,
                    List.of(sessionKey(sessionId)),
                    presentedTokenHash,
                    newTokenHash,
                    String.valueOf(now.toEpochMilli()),
                    String.valueOf(refreshTtl.toMillis()),
                    String.valueOf(reuseGrace.toMillis())));
    long code = result == null || result.isEmpty() ? 0 : ((Number) result.get(0)).longValue();
    if (code == 0) {
      return new Rotation(Rotation.Status.MISSING, null, null);
    }
    Long userId = Long.valueOf(result.get(1).toString());
    if (code < 0) {
      return new Rotation(Rotation.Status.REUSED, userId, null);
    }
    return new Rotation(Rotation.Status.ROTATED, userId, Instant.ofEpochMilli(code));
  }

  @Override
  public boolean exists(String sessionId) {
    return Boolean.TRUE.equals(call(() -> redisTemplate.hasKey(sessionKey(sessionId))));
  }

  @Override
  @Nullable
  public Instant getAuthenticatedAt(String sessionId) {
    Object authenticatedAt =
        call(() -> redisTemplate.opsForHash().get(sessionKey(sessionId), "authenticatedAt"));
    return authenticatedAt == null
        ? null
        : Instant.ofEpochMilli(Long.parseLong(authenticatedAt.toString()));
  }

  @Override
  @Nullable
  public Long findUserId(String sessionId, String tokenHash) {
    List<Object> values =
        call(
            () ->
                redisTemplate
                    .opsForHash()
                    .multiGet(sessionKey(sessionId), List.of("userId", "tokenHash")));
    if (values == null || values.get(0) == null || values.get(1) == null) {
      return null;
    }
    boolean current =
        MessageDigest.isEqual(
            values.get(1).toString().getBytes(StandardCharsets.US_ASCII),
            tokenHash.getBytes(StandardCharsets.US_ASCII));
    return current ? Long.valueOf(values.get(0).toString()) : null;
  }

  @Override
  public void delete(String sessionId) {
    call(() -> redisTemplate.delete(sessionKey(sessionId)));
  }

  @Override
  public List<String> deleteAll(Long userId) {
    return call(
        () -> {
          String userSessionsKey = userSessionsKey(userId);
          Set<String> sessionIds = redisTemplate.opsForZSet().range(userSessionsKey, 0, -1);
          redisTemplate.delete(userSessionsKey);
          if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
          }
          redisTemplate.delete(sessionIds.stream().map(this::sessionKey).toList());
          return List.copyOf(sessionIds);
        });
  }

  private String sessionKey(String sessionId) {
    return SESSION_KEY_PREFIX + sessionId;
  }

  private String userSessionsKey(Long userId) {
    return USER_SESSIONS_KEY_FORMAT.formatted(userId);
  }

  // Redis 장애를 401이 아닌 503으로 알려야 프론트엔드가 로그아웃 처리하지 않는다
  private <T> T call(Supplier<T> operation) {
    try {
      return operation.get();
    } catch (DataAccessException e) {
      throw AuthException.forSessionStoreUnavailable(e);
    }
  }
}
