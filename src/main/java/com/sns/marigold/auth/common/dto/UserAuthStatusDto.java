package com.sns.marigold.auth.common.dto;

import java.util.List;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import io.hypersistence.tsid.TSID;

@Getter
public class UserAuthStatusDto {
  private final String userId;
  // private final Long userId;
  private final List<String> authorities;

  public UserAuthStatusDto(Long userId, List<? extends GrantedAuthority> authorities) {
    this.userId = (userId != null) ? TSID.from(userId).toString() : null;
    // this.userId = userId;
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();
  }
}
