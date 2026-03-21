package com.sns.marigold.auth.common.dto;

import io.hypersistence.tsid.TSID;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
public class UserAuthStatusDto {
  // JSON 변환 시 자동으로 String
  //  @JsonSerialize(using = ToStringSerializer.class)
  // private final Long userId;
  private final String userId;
  private final List<String> authorities;

  public UserAuthStatusDto(Long userId, List<? extends GrantedAuthority> authorities) {
    //     this.userId = userId;
    this.userId = (userId != null) ? TSID.from(userId).toString() : null; // Crockford's BASE32
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();
  }
}
