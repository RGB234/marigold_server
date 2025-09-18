package com.sns.marigold.volunteering.service;

import com.sns.marigold.volunteering.dto.RecruitmentCreateDto;
import com.sns.marigold.volunteering.dto.RecruitmentResponseDto;
import com.sns.marigold.volunteering.dto.RecruitmentUpdateDto;
import java.util.List;

public interface VolunteeringService {

  public void createRecruitment(RecruitmentCreateDto dto);

  public void createReport();


  public void updateRecruitment(RecruitmentUpdateDto dto);


  public void updateReport();


  public void deleteRecruitment(Long recruitmentId);


  public void deleteReport(Long reportId);


  public List<RecruitmentResponseDto> getAllRecruitments();


  public void getReports(Long recruitmentId);

}
