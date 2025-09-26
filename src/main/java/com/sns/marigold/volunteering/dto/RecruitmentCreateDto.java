package com.sns.marigold.volunteering.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class RecruitmentCreateDto {

  @NotBlank(message = "모집 위치를 입력해주세요")
  private String location;

  @NotNull
  @Future(message = "현재 날짜 이후로 입력해주세요")
  private LocalDateTime date;

  @NotBlank
  @Size(min = 10, max = 500, message = "10자 이상 500자 이하로 입력해주세요")
  private String text;

  @NotBlank
  private UUID writerId;
}