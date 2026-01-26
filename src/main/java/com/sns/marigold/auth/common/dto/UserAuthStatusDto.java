package com.sns.marigold.auth.common.dto;

import java.util.List;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class UserAuthStatusDto {

  // private final UUID userId;
  private final Long userId;
  private final List<String> authorities;

  public UserAuthStatusDto(Long userId, List<? extends GrantedAuthority> authorities) {
    // this.userId = (userId != null) ?userId.toString().replace("-", "") : null;
    this.userId = (userId != null) ? userId : null;
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();

  }
}
