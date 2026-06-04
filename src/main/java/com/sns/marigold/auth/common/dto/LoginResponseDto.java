package com.sns.marigold.auth.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "로그인 또는 토큰 재발급 응답")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
  @Schema(description = "API 인증에 사용할 JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
  private String accessToken;
}
