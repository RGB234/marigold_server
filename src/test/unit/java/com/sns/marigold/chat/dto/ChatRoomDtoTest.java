package com.sns.marigold.chat.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.global.tsid.TsidCodec;

class ChatRoomDtoTest {

  @Test
  void serializeTsidIdAsUppercaseString() throws Exception {
    Long roomId = 991000000000000004L;

    String json = new ObjectMapper().writeValueAsString(ChatRoomDto.builder().id(roomId).build());

    assertThat(json).contains("\"id\":\"" + TsidCodec.encode(roomId) + "\"");
  }

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
    String receiverTsid = TsidCodec.encode(receiverId);
    String json = "{\"adoptionPostId\":\"123\",\"receiverId\":\"" + receiverTsid + "\"}";

    NewChatDto dto = new ObjectMapper().readValue(json, NewChatDto.class);

    assertThat(dto.getAdoptionPostId()).isEqualTo(123L);
    assertThat(dto.getReceiverId()).isEqualTo(receiverId);
  }

  @Test
  void rejectDecimalReceiverIdFallback() {
    String json = "{\"adoptionPostId\":\"123\",\"receiverId\":\"123\"}";

    assertThatThrownBy(() -> new ObjectMapper().readValue(json, NewChatDto.class))
        .isInstanceOf(JsonProcessingException.class);
  }

  @Test
  void rejectNumericJsonTokenForTsidId() {
    String json = "{\"adoptionPostId\":\"123\",\"receiverId\":1234567890123}";

    assertThatThrownBy(() -> new ObjectMapper().readValue(json, NewChatDto.class))
        .isInstanceOf(JsonProcessingException.class);
  }
}
