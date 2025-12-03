package com.sns.marigold.user.dto.response;

import com.sns.marigold.user.entity.User;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoDto {
  private final String nickname;

  public static UserInfoDto from(User user) {
    if (user == null) return emptyDto();
    return new UserInfoDto(user.getNickname());
  }

  public static UserInfoDto emptyDto() {
    return new UserInfoDto(null);
  }
}
