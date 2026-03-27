package com.sns.marigold.adoption.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;
import com.sns.marigold.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdoptionCandidateDto {
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long id;
  
  private String nickname;
  private String imageUrl;

  public static AdoptionCandidateDto from(User user, String imageUrl) {
    return AdoptionCandidateDto.builder()
        .id(user.getId())
        .nickname(user.getNickname())
        .imageUrl(imageUrl)
        .build();
  }
}
