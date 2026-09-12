package com.sns.marigold.chat.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hypersistence.tsid.TSID;

class ChatRoomDtoTest {

  @Test
  void serializePostIdAsTsidString() throws Exception {
    Long postId = 991000000000000001L;

    ChatRoomDto dto = ChatRoomDto.builder().postId(postId).build();

    String json = new ObjectMapper().writeValueAsString(dto);

    assertThat(json).contains("\"postId\":\"" + TSID.from(postId) + "\"");
    assertThat(json).doesNotContain("\"postId\":" + postId);
  }
}
