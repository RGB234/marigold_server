package com.sns.marigold.adoption.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;
import com.sns.marigold.user.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "입양 후보자 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdoptionCandidateDto {
  @Schema(description = "TSID 형식 후보자 사용자 ID", type = "string", example = "01JABCDEF1234")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long id;

  @Schema(description = "후보자 닉네임", example = "입양희망자")
  private String nickname;

  @Schema(
      description = "후보자 프로필 이미지 URL",
      example = "https://example.com/profile.jpg",
      nullable = true)
  private String imageUrl;

  public static AdoptionCandidateDto from(User user, String imageUrl) {
    return AdoptionCandidateDto.builder()
        .id(user.getId())
        .nickname(user.getDisplayNickname())
        .imageUrl(imageUrl)
        .build();
  }
}
