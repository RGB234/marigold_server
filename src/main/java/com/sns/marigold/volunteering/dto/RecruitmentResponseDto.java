package com.sns.marigold.volunteering.dto;

import com.sns.marigold.user.dto.InstitutionUserResponseDto;
import com.sns.marigold.volunteering.entity.VolunteeringRecruitment;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentResponseDto {

  private Long id;
  private LocalDateTime createdAt;
  private LocalDateTime modifiedAt;

  private String location;

  private LocalDateTime date;

  private String text;

  private UUID writerId;

  public static RecruitmentResponseDto from(VolunteeringRecruitment entity) {
    return RecruitmentResponseDto
      .builder()
      .id(entity.getId())
      .createdAt(entity.getCreatedAt())
      .modifiedAt(entity.getModifiedAt())
      .location(entity.getLocation())
      .date(entity.getDate())
      .text(entity.getText())
      .writerId(entity.getWriter().getId())
      .build();
  }

  ;
}
