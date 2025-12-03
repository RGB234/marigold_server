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
  private final UUID id;
  private final String username;
  private final String password;

  private final OAuth2UserInfo oAuth2UserInfo;

  private final Collection<SimpleGrantedAuthority> authorities;
  // === 생성자 (OAuth2 로그인용) ===
  public CustomPrincipal(OAuth2UserInfo oAuth2UserInfo, Collection<SimpleGrantedAuthority> authorities) {
    this.id = null;
    this.username = null;
    this.password = null;

    this.oAuth2UserInfo = oAuth2UserInfo;

    this.authorities = authorities;
  }

  // === OAuth2User 구현부 ===
  @Override
  public Map<String, Object> getAttributes() {
    return Objects.requireNonNull(oAuth2UserInfo).getAttributes();
  }

  @Override
  public String getName() {
    return id != null ? id.toString() : Objects.requireNonNull(oAuth2UserInfo).getName();
  }
}
