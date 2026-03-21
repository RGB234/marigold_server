package com.sns.marigold.global;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlConstants {

  // API versioning prefix
  public static final String API_V1 = "/api/v1";

  // Base paths for each domain
  public static final String AUTH_BASE = API_V1 + "/auth";
  public static final String USER_BASE = API_V1 + "/user";
  public static final String ADOPTION_BASE = API_V1 + "/adoption";
  public static final String CHAT_BASE = API_V1 + "/chat";
  public static final String STORAGE_BASE = API_V1 + "/storage";
}
