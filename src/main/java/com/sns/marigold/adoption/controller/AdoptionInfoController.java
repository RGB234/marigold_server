package com.sns.marigold.adoption.controller;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @AuthenticationPrincipal CustomPrincipal principal
  ) {
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


  @GetMapping("/")
  public Page<AdoptionInfoResponseDto> search(
      @Valid AdoptionInfoSearchFilterDto dto,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return adoptionInfoService.search(dto, page, size);
  }
}