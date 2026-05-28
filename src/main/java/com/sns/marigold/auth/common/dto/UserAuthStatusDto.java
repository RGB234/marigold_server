package com.sns.marigold.auth.common.dto;

import io.hypersistence.tsid.TSID;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class UserAuthStatusDto {
  private final String userId;
  private final List<String> authorities;
  private final boolean refreshTokenPresent;

  public UserAuthStatusDto(
      Long userId, List<? extends GrantedAuthority> authorities, boolean refreshTokenPresent) {
    this.userId = (userId != null) ? TSID.from(userId).toString() : null; // Crockford's BASE32
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    this.refreshTokenPresent = refreshTokenPresent;
  }
}
