package com.sns.marigold.auth.common.recent;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class InMemoryRecentAuthStore implements RecentAuthStore {

  private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

  @Override
  public void save(String token, Long userId, Instant expiresAt) {
    entries.put(token, new Entry(userId, expiresAt));
  }

  @Override
  public boolean isValid(String token, Long expectedUserId, Instant now) {
    Entry entry = entries.get(token);
    if (entry == null) {
      return false;
    }

    if (!entry.expiresAt().isAfter(now)) {
      entries.remove(token);
      return false;
    }

    return Objects.equals(entry.userId(), expectedUserId);
  }

  @Override
  public void remove(String token) {
    entries.remove(token);
  }

  private record Entry(Long userId, Instant expiresAt) {}
}
