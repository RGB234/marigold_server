package com.sns.marigold.auth.common;

import com.sns.marigold.auth.oauth2.OAuth2UserInfo;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
public class CustomPrincipal implements OAuth2User {
  private final UUID userId;
  private final OAuth2UserInfo oAuth2UserInfo;
  private final Collection<SimpleGrantedAuthority> authorities;
  
  public CustomPrincipal(UUID userId, OAuth2UserInfo oAuth2UserInfo, Collection<SimpleGrantedAuthority> authorities) {
    this.userId = userId;
    this.oAuth2UserInfo = oAuth2UserInfo;
    this.authorities = authorities;
  }
  
  @Override
  public Map<String, Object> getAttributes() {
    return Objects.requireNonNull(oAuth2UserInfo).getAttributes();
  }

  @Override
  public String getName() {
    return userId != null ? userId.toString() : "";
  }
}
