package com.sns.marigold.user.dto;

import com.sns.marigold.user.entity.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class UserProfileDTO {

  @NotBlank
  @Size(min = 3, max = 12)
  private String username;


  public static UserProfileDTO fromUserEntity(UserEntity user) {
    return UserProfileDTO.builder()
      .username(user.getUsername())
      .build();
  }
}
