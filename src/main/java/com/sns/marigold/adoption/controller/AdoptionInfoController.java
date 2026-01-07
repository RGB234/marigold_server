package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionDetailResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.service.AdoptionInfoService;
import com.sns.marigold.auth.common.CustomPrincipal;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/adoption")
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoController {

  private final AdoptionInfoService adoptionInfoService;

  @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> create(
      @ModelAttribute @Valid AdoptionInfoCreateDto dto,
      BindingResult bindingResult,
      @AuthenticationPrincipal CustomPrincipal principal) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
    }

    UUID userId = principal.getUserId();
    if (userId == null) {
      return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다.");
    }

    adoptionInfoService.create(dto, userId);
    return ResponseEntity.ok().body("Adoption info created successfully");
  }

  @GetMapping("")
  // Pageable을 사용하면 ?page=0&size=10&sort=id,desc 처럼 정렬도 자동 지원됩니다.
  // @PageableDefault로 기본값 설정 가능
  // @ModelAttribute: 쿼리 파라미터가 없어도 빈 DTO 객체 생성
  public ResponseEntity<Page<AdoptionInfoResponseDto>> search(
      @ModelAttribute AdoptionInfoSearchFilterDto dto,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
    Page<AdoptionInfoResponseDto> result = adoptionInfoService.search(dto, pageable);
    return ResponseEntity.ok().body(result);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AdoptionDetailResponseDto> getDetail(@PathVariable String id) {
    AdoptionDetailResponseDto result = adoptionInfoService.getDetail(Long.parseLong(id));
    return ResponseEntity.ok().body(result);
  }
}