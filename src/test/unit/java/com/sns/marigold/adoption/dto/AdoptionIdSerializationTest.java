package com.sns.marigold.adoption.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AdoptionIdSerializationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serializePostAndCommentIdsAsNumericStrings() throws Exception {
    Long postId = 991000000000000001L;
    Long commentId = 991000000000000002L;

    String summaryJson =
        objectMapper.writeValueAsString(AdoptionPostDto.builder().id(postId).build());
    String detailJson =
        objectMapper.writeValueAsString(AdoptionPostDetailDto.builder().id(postId).build());
    String commentJson =
        objectMapper.writeValueAsString(
            AdoptionCommentResponseDto.builder().id(commentId).adoptionPostId(postId).build());

    assertThat(summaryJson).contains("\"id\":\"" + postId + "\"");
    assertThat(detailJson).contains("\"id\":\"" + postId + "\"");
    assertThat(commentJson)
        .contains("\"id\":\"" + commentId + "\"")
        .contains("\"adoptionPostId\":\"" + postId + "\"");
  }
}
