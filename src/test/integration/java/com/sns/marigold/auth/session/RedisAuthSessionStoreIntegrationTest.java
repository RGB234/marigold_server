package com.sns.marigold.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.sns.marigold.auth.common.session.AuthSessionStore;
import com.sns.marigold.auth.common.session.AuthSessionStore.Rotation;
import com.sns.marigold.support.BaseIntegrationTest;

class RedisAuthSessionStoreIntegrationTest extends BaseIntegrationTest {

  private static final Duration REFRESH_TTL = Duration.ofDays(1);
  private static final Duration GRACE = Duration.ofSeconds(10);

  @Autowired private AuthSessionStore store;
  @Autowired private StringRedisTemplate redisTemplate;

  private Instant now;

  // 필드 초기화는 Spring 컨텍스트 기동 전에 일어나므로 테스트마다 현재 시각을 다시 잡는다
  @BeforeEach
  void setUpNow() {
    now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }

  @Test
  @DisplayName("세션은 refresh 만료 시각에 Redis에서 사라지도록 TTL을 건다.")
  void create_SetsTtlToRefreshExpiry() {
    create("s1", 1L, "t1", now.plus(Duration.ofDays(14)));

    assertThat(store.exists("s1")).isTrue();
    // Redis는 TTL을 초 단위로 반올림하고 컨테이너 시계가 호스트와 조금 어긋날 수 있어 양쪽에 여유를 둔다
    assertThat(redisTemplate.getExpire("auth:session:s1", TimeUnit.SECONDS))
        .isCloseTo(REFRESH_TTL.toSeconds(), within(5L));
  }

  @Test
  @DisplayName("로그인 시각은 refresh로 회전해도 바뀌지 않는다.")
  void authenticatedAt_SurvivesRotation() {
    create("s1", 1L, "t1", now.plus(Duration.ofDays(14)));

    store.rotate("s1", "t1", "t2", now.plusSeconds(60), REFRESH_TTL, GRACE);

    assertThat(store.getAuthenticatedAt("s1")).isEqualTo(now);
    assertThat(store.getAuthenticatedAt("none")).isNull();
  }

  @Test
  @DisplayName("현재 token의 해시일 때만 세션의 사용자를 돌려준다.")
  void findUserId_MatchesOnlyCurrentToken() {
    create("s1", 7L, "t1", now.plus(Duration.ofDays(14)));
    store.rotate("s1", "t1", "t2", now, REFRESH_TTL, GRACE);

    assertThat(store.findUserId("s1", "t2")).isEqualTo(7L);
    assertThat(store.findUserId("s1", "t1")).isNull();
    assertThat(store.findUserId("none", "t2")).isNull();
  }

  @Test
  @DisplayName("현재 token이면 회전하고, 새 token이 현재 token이 된다.")
  void rotate_CurrentTokenRotates() {
    create("s1", 1L, "t1", now.plus(Duration.ofDays(14)));

    Rotation first = store.rotate("s1", "t1", "t2", now, REFRESH_TTL, GRACE);
    Rotation second = store.rotate("s1", "t2", "t3", now.plusSeconds(60), REFRESH_TTL, GRACE);

    assertThat(first).isEqualTo(new Rotation(Rotation.Status.ROTATED, 1L, now.plus(REFRESH_TTL)));
    assertThat(second.status()).isEqualTo(Rotation.Status.ROTATED);
  }

  @Test
  @DisplayName("응답을 못 받은 재시도처럼 직전 token이 유예 안에 오면 다시 회전해 준다.")
  void rotate_PreviousTokenWithinGraceRotatesAgain() {
    create("s1", 1L, "t1", now.plus(Duration.ofDays(14)));
    store.rotate("s1", "t1", "t2", now, REFRESH_TTL, GRACE);

    Rotation retry = store.rotate("s1", "t1", "t3", now.plusSeconds(5), REFRESH_TTL, GRACE);
    Rotation next = store.rotate("s1", "t3", "t4", now.plusSeconds(6), REFRESH_TTL, GRACE);

    assertThat(retry.status()).isEqualTo(Rotation.Status.ROTATED);
    assertThat(next.status()).isEqualTo(Rotation.Status.ROTATED);
  }

  @Test
  @DisplayName("직전 token이 유예 뒤에 오면 탈취로 보고 세션을 지운다.")
  void rotate_PreviousTokenAfterGraceRevokesSession() {
    create("s1", 1L, "t1", now.plus(Duration.ofDays(14)));
    store.rotate("s1", "t1", "t2", now, REFRESH_TTL, GRACE);

    Rotation late = store.rotate("s1", "t1", "t3", now.plusSeconds(11), REFRESH_TTL, GRACE);

    assertThat(late.status()).isEqualTo(Rotation.Status.REUSED);
    assertThat(store.exists("s1")).isFalse();
  }

  @Test
  @DisplayName("두 번 이상 지난 token이 오면 유예와 상관없이 세션을 지운다.")
  void rotate_OlderTokenRevokesSession() {
    create("s1", 1L, "t1", now.plus(Duration.ofDays(14)));
    store.rotate("s1", "t1", "t2", now, REFRESH_TTL, GRACE);
    store.rotate("s1", "t2", "t3", now.plusSeconds(1), REFRESH_TTL, GRACE);

    Rotation reused = store.rotate("s1", "t1", "t4", now.plusSeconds(2), REFRESH_TTL, GRACE);

    assertThat(reused).isEqualTo(new Rotation(Rotation.Status.REUSED, 1L, null));
    assertThat(store.exists("s1")).isFalse();
  }

  @Test
  @DisplayName("회전해도 만료 시각은 로그인 후 절대 만료를 넘지 않는다.")
  void rotate_NeverExtendsPastAbsoluteExpiry() {
    Instant absoluteExpiresAt = now.plusSeconds(30);
    store.create("s1", 1L, "t1", now, absoluteExpiresAt, absoluteExpiresAt);

    Rotation rotation = store.rotate("s1", "t1", "t2", now, REFRESH_TTL, GRACE);

    assertThat(rotation).isEqualTo(new Rotation(Rotation.Status.ROTATED, 1L, absoluteExpiresAt));
  }

  @Test
  @DisplayName("없는 세션은 MISSING을 돌려준다.")
  void rotate_MissingSession() {
    assertThat(store.rotate("none", "t1", "t2", now, REFRESH_TTL, GRACE).status())
        .isEqualTo(Rotation.Status.MISSING);
  }

  @Test
  @DisplayName("사용자의 세션만 모두 지우고 지운 세션 ID를 돌려준다.")
  void deleteAll_RemovesOnlyThatUsersSessions() {
    create("a", 1L, "t1", now.plus(Duration.ofDays(14)));
    create("b", 1L, "t2", now.plus(Duration.ofDays(14)));
    create("other", 2L, "t3", now.plus(Duration.ofDays(14)));

    assertThat(store.deleteAll(1L)).containsExactlyInAnyOrder("a", "b");
    assertThat(store.exists("a")).isFalse();
    assertThat(store.exists("b")).isFalse();
    assertThat(store.exists("other")).isTrue();
    assertThat(store.deleteAll(1L)).isEmpty();
  }

  private void create(String sessionId, Long userId, String tokenId, Instant absoluteExpiresAt) {
    store.create(sessionId, userId, tokenId, now, now.plus(REFRESH_TTL), absoluteExpiresAt);
  }
}
