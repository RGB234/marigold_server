package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PersonalUserUpdateDto {

  @Size(min = 3, max = 12)
  private String username;
}
