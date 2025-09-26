package com.sns.marigold.volunteering.service;

import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.service.InstitutionUserService;
import com.sns.marigold.volunteering.dto.RecruitmentCreateDto;
import com.sns.marigold.volunteering.dto.RecruitmentResponseDto;
import com.sns.marigold.volunteering.dto.RecruitmentUpdateDto;
import com.sns.marigold.volunteering.entity.VolunteeringRecruitment;
import com.sns.marigold.volunteering.repository.VolunteeringRecruitmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VolunteeringService {

  private final InstitutionUserService institutionUserService;
  private final VolunteeringRecruitmentRepository recruitmentRepository;

  public void createRecruitment(RecruitmentCreateDto dto) {
    InstitutionUser writer = institutionUserService.findById(dto.getWriterId());

    VolunteeringRecruitment recruitment = VolunteeringRecruitment.builder(
      ).location(dto.getLocation())
      .date(dto.getDate())
      .text(dto.getText()).
      writer(writer).
      build();

    recruitmentRepository.save(recruitment);
  }

  public void createReport() {

  }

  public void updateRecruitment(RecruitmentUpdateDto dto) {
    InstitutionUser writer = institutionUserService.findById(dto.getWriterId());

    VolunteeringRecruitment recruitment = VolunteeringRecruitment.builder(
      ).location(dto.getLocation())
      .date(dto.getDate())
      .text(dto.getText()).
      writer(writer).
      build();

    recruitmentRepository.save(recruitment);
  }

  public void updateReport() {

  }

  public void deleteRecruitment(Long recruitmentId) {

  }

  public void deleteReport(Long reportId) {

  }

  public List<RecruitmentResponseDto> getAllRecruitments() {
    List<VolunteeringRecruitment> recruitments = recruitmentRepository.findAll();
    return recruitments.stream().map(RecruitmentResponseDto::from).toList();
  }

  public void getReports(Long recruitmentId) {

  }
}
