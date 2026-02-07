package com.sns.marigold.user.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.sns.marigold.user.entity.User;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class UserInfoDto {
  @JsonSerialize(using = ToStringSerializer.class)
  private final Long id;
  private final String nickname;
  private final String imageUrl;

  public static UserInfoDto from(User user) {
    if (user == null) return emptyDto();
    return UserInfoDto.builder()
        .id(user.getId())
        .nickname(user.getNickname())
        .imageUrl(user.getImage() != null ? user.getImage().getStoreFileName() : null)
        .build();
  }

  public static UserInfoDto emptyDto() {
    return UserInfoDto.builder()
        .id(null)
        .nickname(null)
        .imageUrl(null)
        .build();
  }
}
