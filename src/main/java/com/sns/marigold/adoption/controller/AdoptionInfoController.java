package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionDetailResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionInfoUpdateDto;
import com.sns.marigold.adoption.service.AdoptionInfoService;
import com.sns.marigold.auth.common.CustomPrincipal;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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

    if (principal == null || principal.getUserId() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 정보를 찾을 수 없습니다.");
    }
    Long userId = principal.getUserId();
    Long adoptionInfoId = adoptionInfoService.create(dto, userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("id", adoptionInfoId, "message", "Adoption info created successfully"));
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

  @GetMapping("/writer/{userId}")
  public ResponseEntity<Page<AdoptionInfoResponseDto>> searchByWriter(@PathVariable Long userId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
    Page<AdoptionInfoResponseDto> result = adoptionInfoService.searchByWriter(userId, pageable);
    return ResponseEntity.ok().body(result);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AdoptionDetailResponseDto> getDetail(@PathVariable String id) {
    try {
      AdoptionDetailResponseDto result = adoptionInfoService.getDetail(Long.parseLong(id));
      return ResponseEntity.ok().body(result);
    } catch (EntityNotFoundException e) {
      return ResponseEntity.notFound().build();
    }

  }

  @PatchMapping("/{id}")
  public ResponseEntity<?> update(@NonNull @AuthenticationPrincipal CustomPrincipal principal,
      @NonNull @PathVariable Long id, @Valid @ModelAttribute AdoptionInfoUpdateDto dto,
      BindingResult bindingResult) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
    }

    Long userId = principal.getUserId();
    if (userId == null) {
      return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다.");
    }

    Objects.requireNonNull(dto, "dto cannot be null");
    adoptionInfoService.update(id, userId, dto);

    return ResponseEntity.ok().body("Adoption info updated successfully");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> delete(@NonNull @AuthenticationPrincipal CustomPrincipal principal,
      @NonNull @PathVariable Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      return ResponseEntity.badRequest().body("사용자 정보를 찾을 수 없습니다.");
    }
    adoptionInfoService.delete(id, userId);
    return ResponseEntity.ok().body("Adoption info deleted successfully");
  }
}