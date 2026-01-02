package com.sns.marigold.auth.common.dto;

import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class UserAuthStatusDto {

  private final boolean authenticated;
  private final List<String> authorities;

  public UserAuthStatusDto(boolean authenticated, List<? extends GrantedAuthority> authorities) {
    this.authenticated = authenticated;
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();

  }
}
