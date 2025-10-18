package com.sns.marigold.user.dto;

import com.sns.marigold.user.entity.AdminUser;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUserResponseDto {
  private String nickname;

  public static AdminUserResponseDto fromUser(AdminUser user) {
    return AdminUserResponseDto.builder()
        .nickname(user.getNickname())
        .build();
  }
}
