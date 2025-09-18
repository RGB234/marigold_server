package com.sns.marigold.volunteering.service;

import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.PersonalUserRepository;
import com.sns.marigold.user.service.InstitutionUserService;
import com.sns.marigold.user.service.PersonalUserService;
import com.sns.marigold.volunteering.dto.RecruitmentCreateDto;
import com.sns.marigold.volunteering.dto.RecruitmentResponseDto;
import com.sns.marigold.volunteering.dto.RecruitmentUpdateDto;
import com.sns.marigold.volunteering.entity.VolunteeringRecruitment;
import com.sns.marigold.volunteering.repository.VolunteeringRecruitmentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VolunteeringServiceImpl implements VolunteeringService {

  final InstitutionUserService userService;
  final VolunteeringRecruitmentRepository recruitmentRepository;

  @Override
  public void createRecruitment(RecruitmentCreateDto dto) {
    InstitutionUser writer = userService.findById(dto.getWriterId());

    VolunteeringRecruitment recruitment = VolunteeringRecruitment.builder(
      ).location(dto.getLocation())
      .date(dto.getDate())
      .text(dto.getText()).
      writer(writer).
      build();

    recruitmentRepository.save(recruitment);
  }

  @Override
  public void createReport() {

  }

  @Override
  public void updateRecruitment(RecruitmentUpdateDto dto) {
    InstitutionUser writer = userService.findById(dto.getWriterId());

    VolunteeringRecruitment recruitment = VolunteeringRecruitment.builder(
      ).location(dto.getLocation())
      .date(dto.getDate())
      .text(dto.getText()).
      writer(writer).
      build();

    recruitmentRepository.save(recruitment);
  }

  @Override
  public void updateReport() {

  }

  @Override
  public void deleteRecruitment(Long recruitmentId) {

  }

  @Override
  public void deleteReport(Long reportId) {

  }

  @Override
  public List<RecruitmentResponseDto> getAllRecruitments() {
    List<VolunteeringRecruitment> recruitments = recruitmentRepository.findAll();
    return recruitments.stream().map(RecruitmentResponseDto::fromEntity).toList();
  }

  @Override
  public void getReports(Long recruitmentId) {

  }
}
