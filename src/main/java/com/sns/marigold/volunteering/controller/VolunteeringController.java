package com.sns.marigold.volunteering.controller;


import com.sns.marigold.volunteering.dto.RecruitmentCreateDto;
import com.sns.marigold.volunteering.dto.RecruitmentResponseDto;
import com.sns.marigold.volunteering.dto.RecruitmentUpdateDto;
import com.sns.marigold.volunteering.service.VolunteeringServiceImpl;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/volunteering")
@Slf4j
public class VolunteeringController {

  final VolunteeringServiceImpl volunteeringService;

  @PostMapping("/recruitment/create")
  public void createRecruitment(@RequestBody @Valid RecruitmentCreateDto dto) {
    volunteeringService.createRecruitment(dto);
  }

  @PatchMapping("/recruitment/update")
  public void updateRecruitment(@RequestBody @Valid RecruitmentUpdateDto dto) {
    volunteeringService.updateRecruitment(dto);
  }

  @GetMapping("/recruitment")
  public List<RecruitmentResponseDto> getAllRecruitments() {
    return volunteeringService.getAllRecruitments();
  }

  @DeleteMapping("/recruitment/{recruitmentId}")
  public void deleteRecruitment(@PathVariable Long recruitmentId) {
    volunteeringService.deleteRecruitment(recruitmentId);
  }
}
