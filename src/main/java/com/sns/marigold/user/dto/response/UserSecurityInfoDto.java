package com.sns.marigold.user.dto.response;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "사용자 보안 정보")
@Getter
@Builder
@AllArgsConstructor
public class UserSecurityInfoDto {

  @Schema(description = "등록된 이메일", example = "user@example.com", nullable = true)
  private final String email;

  @Schema(description = "이메일/비밀번호 로그인 수단 보유 여부", example = "true")
  private final boolean hasLocalCredentials;

  @Schema(description = "연동된 OAuth2 제공자", example = "KAKAO", nullable = true)
  private final ProviderInfo providerInfo;

  @Schema(description = "OAuth2 계정 연동 여부", example = "true")
  private final boolean hasOAuth2Link;

  public static UserSecurityInfoDto from(User user) {
    return UserSecurityInfoDto.builder()
        .email(user.getEmail())
        .hasLocalCredentials(user.hasLocalCredentials())
        .providerInfo(user.getProviderInfo())
        .hasOAuth2Link(user.hasOAuth2Link())
        .build();
  }
}
