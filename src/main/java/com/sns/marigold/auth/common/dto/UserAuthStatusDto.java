package com.sns.marigold.auth.common.dto;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;

import io.hypersistence.tsid.TSID;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "현재 인증 상태 응답")
@Getter
public class UserAuthStatusDto {
  @Schema(description = "인증된 사용자 ID. 비로그인 상태면 null", example = "01JABCDEF1234", nullable = true)
  private final String userId;

  @Schema(description = "인증된 사용자의 권한 목록", example = "[\"ROLE_USER\"]")
  private final List<String> authorities;

  @Schema(description = "refresh token 쿠키 존재 여부", example = "true")
  private final boolean refreshTokenPresent;

  public UserAuthStatusDto(
      Long userId, List<? extends GrantedAuthority> authorities, boolean refreshTokenPresent) {
    this.userId = (userId != null) ? TSID.from(userId).toString() : null; // Crockford's BASE32
    this.authorities = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    this.refreshTokenPresent = refreshTokenPresent;
  }
}
