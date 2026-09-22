package com.sns.marigold.chat.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hypersistence.tsid.TSID;

class ChatRoomDtoTest {

  @Test
  void serializePostIdAsNumericString() throws Exception {
    Long postId = 991000000000000001L;

    ChatRoomDto dto = ChatRoomDto.builder().postId(postId).build();

    String json = new ObjectMapper().writeValueAsString(dto);

    assertThat(json).contains("\"postId\":\"" + postId + "\"");
    assertThat(json).doesNotContain("\"postId\":" + postId);
  }

  @Test
  void serializeAttachmentIdAsNumericString() throws Exception {
    Long attachmentId = 991000000000000002L;

    ChatAttachmentDto dto = ChatAttachmentDto.builder().id(attachmentId).build();

    String json = new ObjectMapper().writeValueAsString(dto);

    assertThat(json).contains("\"id\":\"" + attachmentId + "\"");
    assertThat(json).doesNotContain("\"id\":" + attachmentId);
  }

  @Test
  void deserializeNumericPostIdAndTsidReceiverId() throws Exception {
    Long receiverId = 991000000000000003L;
    String receiverTsid = TSID.from(receiverId).toString();
    String json = "{\"adoptionPostId\":\"123\",\"receiverId\":\"" + receiverTsid + "\"}";

    NewChatDto dto = new ObjectMapper().readValue(json, NewChatDto.class);

    assertThat(dto.getAdoptionPostId()).isEqualTo(123L);
    assertThat(dto.getReceiverId()).isEqualTo(receiverId);
  }
}
