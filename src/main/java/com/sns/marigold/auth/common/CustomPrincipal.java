package com.sns.marigold.auth.common;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Getter
public class CustomPrincipal implements OAuth2User {
  private final UUID userId;
  private final Collection<SimpleGrantedAuthority> authorities;
  
  public CustomPrincipal(UUID userId, Collection<SimpleGrantedAuthority> authorities) {
    this.userId = userId;
    this.authorities = authorities;
  }
  
  @Override
  public Map<String, Object> getAttributes() {
    return this.getAttributes();
  }

  @Override
  public String getName() {
    return userId != null ? userId.toString() : "";
  }
}
