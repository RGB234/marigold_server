package com.sns.marigold.user.dto.update;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // Jackson을 위한 public 기본 생성자
public class UserUpdateDto {
  @Size(min = 3, max = 12)
  private String nickname;
}
