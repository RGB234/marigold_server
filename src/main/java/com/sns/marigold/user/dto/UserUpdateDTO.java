package com.sns.marigold.user.dto;

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
public class UserUpdateDTO {

  @NotBlank
  @Size(min = 3, max = 12)
  private String username;
}
