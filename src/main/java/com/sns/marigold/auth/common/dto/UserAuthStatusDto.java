package com.sns.marigold.auth.common.dto;

import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class UserAuthStatusDto {

  private final boolean isAuthenticated;
  private final List<String> authorities;

  public UserAuthStatusDto(boolean isAuthenticated, List<? extends GrantedAuthority> authorities) {
    this.isAuthenticated = isAuthenticated;
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();

  }
}
