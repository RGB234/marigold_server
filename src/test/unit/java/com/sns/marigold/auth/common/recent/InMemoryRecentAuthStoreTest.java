package com.sns.marigold.auth.common.recent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class InMemoryRecentAuthStoreTest {

  @Test
  void isValid_SucceedsUntilExpiration() {
    InMemoryRecentAuthStore store = new InMemoryRecentAuthStore();
    Instant now = Instant.now();

    store.save("token", 1L, now.plusSeconds(300));

    assertThat(store.isValid("token", 1L, now)).isTrue();
    assertThat(store.isValid("token", 1L, now.plusSeconds(1))).isTrue();
  }

  @Test
  void isValid_FailsForDifferentUser() {
    InMemoryRecentAuthStore store = new InMemoryRecentAuthStore();
    Instant now = Instant.now();

    store.save("token", 1L, now.plusSeconds(300));

    assertThat(store.isValid("token", 2L, now)).isFalse();
  }

  @Test
  void isValid_FailsForExpiredToken() {
    InMemoryRecentAuthStore store = new InMemoryRecentAuthStore();
    Instant now = Instant.now();

    store.save("token", 1L, now.minusSeconds(1));

    assertThat(store.isValid("token", 1L, now)).isFalse();
  }
}
