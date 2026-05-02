package com.sns.marigold.auth.common.recent;

import java.time.Instant;

public interface RecentAuthStore {

  void save(String token, Long userId, Instant expiresAt);

  boolean isValid(String token, Long expectedUserId, Instant now);

  void remove(String token);
}
