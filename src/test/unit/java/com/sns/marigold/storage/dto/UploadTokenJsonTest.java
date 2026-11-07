package com.sns.marigold.storage.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.chat.dto.ChatFileMessageRequestDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;

// 프론트 계약은 토큰을 문자열로 주고받는다. 레코드로 바꿔도 JSON 모양이 그대로인지 확인한다
class UploadTokenJsonTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void readsTokenListFromStringArray() throws Exception {
    ChatFileMessageRequestDto request =
        objectMapper.readValue(
            "{\"uploadTokens\":[\"token-a\",\"token-b\"]}", ChatFileMessageRequestDto.class);

    assertThat(request.getUploadTokens())
        .containsExactly(new UploadToken("token-a"), new UploadToken("token-b"));
  }

  @Test
  void readsSingleTokenFromString() throws Exception {
    UserUpdateDto request =
        objectMapper.readValue("{\"imageUploadToken\":\"token-a\"}", UserUpdateDto.class);

    assertThat(request.getImageUploadToken()).isEqualTo(new UploadToken("token-a"));
  }

  @Test
  void writesTokenAsString() throws Exception {
    String json =
        objectMapper.writeValueAsString(
            UploadIssueResponseDto.builder().uploadToken(new UploadToken("token-a")).build());

    assertThat(json).contains("\"uploadToken\":\"token-a\"");
  }
}
