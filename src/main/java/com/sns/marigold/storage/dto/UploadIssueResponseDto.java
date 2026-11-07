package com.sns.marigold.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "업로드 URL 발급 결과. 요청한 파일 순서와 같다.")
@Getter
@Builder
@AllArgsConstructor
public class UploadIssueResponseDto {
  @Schema(description = "파일을 PUT할 URL. 인증 쿠키나 CSRF 헤더 없이 요청해야 한다.")
  private String uploadUrl;

  @Schema(description = "PUT 요청의 Content-Type 헤더 값. 다르면 업로드가 거부될 수 있다.", example = "image/jpeg")
  private String contentType;

  @Schema(description = "업로드 후 도메인 API에 보낼 토큰")
  private UploadToken uploadToken;
}
