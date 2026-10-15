package com.sns.marigold.auth.common.dto;

import java.util.List;

import com.sns.marigold.global.tsid.TsidId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "인증된 사용자 식별 정보")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthPrincipalDto {
  @TsidId
  @Schema(description = "인증된 사용자 ID", type = "string", example = "01JABCDEF1234")
  private Long userId;

  @Schema(description = "사용자 권한 목록", example = "[\"ROLE_PERSON\"]")
  private List<String> authorities;
}
