package com.sns.marigold.user.dto.create;

import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.global.annotation.EnumType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "OAuth2 회원가입 요청")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2SignupDto {
  @Schema(description = "OAuth2 제공자", example = "KAKAO")
  @EnumType(target = ProviderInfo.class)
  @NotNull(message = "소셜 로그인 제공자는 필수입니다.")
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @Schema(description = "OAuth2 제공자가 전달한 사용자 식별자", example = "123456789")
  @NotBlank(message = "소셜 로그인 계정 ID는 필수입니다.")
  private String providerId; // 소셜로그인 계정 id

  @Schema(description = "가입할 사용자 권한", example = "ROLE_USER")
  @EnumType(target = Role.class)
  @NotNull(message = "사용자 권한은 필수입니다.")
  private Role role; // 사용자 권한
}
