package com.sns.marigold.auth.common.dto;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class UserAuthStatusDto {

  // private final UUID userId;
  private final String userId;
  private final List<String> authorities;

  public UserAuthStatusDto(UUID userId, List<? extends GrantedAuthority> authorities) {
    // this.userId = (userId != null) ?userId.toString().replace("-", "") : null;
    this.userId = (userId != null) ? userId.toString() : null;
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();

  }
}
