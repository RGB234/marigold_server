package com.sns.marigold.storage.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 클라이언트 직접 업로드의 토큰.
 *
 * <p>클라이언트가 {@link com.sns.marigold.storage.controller.StorageUploadController#issueUploads}({@code
 * POST /storage/uploads})로 먼저 발급받고, 받은 URL로 파일을 PUT한 뒤 도메인 API 본문에 담아 보낸다. 서명만 된 값이라 {@link
 * com.sns.marigold.storage.service.StorageUploadService#confirm}이 검증하기 전에는 믿지 않는다.
 *
 * <p>전체 흐름은 {@code docs/storage.md}, 클라이언트 절차는 {@code docs/contracts/uploads.md}에 있다.
 */
@Schema(type = "string", description = "POST /storage/uploads로 발급받은 업로드 토큰. 파일을 PUT한 뒤 보낸다.")
public record UploadToken(@JsonValue String value) {

  // JSON에서는 객체가 아니라 문자열 하나로 주고받는다
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public UploadToken {}
}
