package com.sns.marigold.auth.common;

import com.sns.marigold.auth.common.enums.AuthStatus;
import java.util.Collection;
import java.util.Map;
import lombok.Getter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class CustomPrincipal implements OAuth2User {
  private final Long userId;
  private final Collection<SimpleGrantedAuthority> authorities;
  private final Map<String, Object> attributes;

  private final AuthStatus authStatus;

  public CustomPrincipal(
      Long userId, Collection<SimpleGrantedAuthority> authorities, Map<String, Object> attributes, AuthStatus authStatus) {
    this.userId = userId;
    this.authorities = authorities;
    this.attributes = attributes;
    this.authStatus = authStatus;
  }

  @Override
  @NonNull
  public Map<String, Object> getAttributes() {
    return attributes != null ? attributes : Map.of();
  }

  @Override
  @NonNull
  public String getName() {
    return userId != null ? userId.toString() : "";
  }
}
