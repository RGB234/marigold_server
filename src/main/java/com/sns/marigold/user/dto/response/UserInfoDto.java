package com.sns.marigold.user.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;
import com.sns.marigold.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserInfoDto {
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private final Long id;

  private final String nickname;

  @Setter private String imageUrl;

  public static UserInfoDto from(User user) {
    if (user == null) return emptyDto();

    return UserInfoDto.builder()
        .id(user.getId())
        .nickname(user.getDisplayNickname())
        .imageUrl(user.getImage() != null ? user.getImage().getStoredFileName() : null)
        .build();
  }

  public static UserInfoDto emptyDto() {
    return UserInfoDto.builder().id(null).nickname(null).imageUrl(null).build();
  }
}
