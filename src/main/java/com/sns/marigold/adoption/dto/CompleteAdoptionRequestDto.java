package com.sns.marigold.adoption.dto;

import com.sns.marigold.global.tsid.TsidId;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "입양 완료 처리 요청")
@Getter
@Setter
@NoArgsConstructor
public class CompleteAdoptionRequestDto {
  @Schema(description = "선택한 입양자 ID", type = "string", example = "01JABCDEF1234")
  @TsidId
  private Long adopterId;
}
