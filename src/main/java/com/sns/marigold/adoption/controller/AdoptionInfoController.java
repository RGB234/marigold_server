package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.service.AdoptionInfoService;
import com.sns.marigold.auth.common.CustomPrincipal;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
@RequestMapping("/adoption")
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoController {

  private final AdoptionInfoService adoptionInfoService;

  @PostMapping("/create")
  public ResponseEntity<?> create(@RequestBody @Valid AdoptionInfoCreateDto dto,
    @SessionAttribute(value = "uid", required = true) String uid,
    BindingResult bindingResult,
    @AuthenticationPrincipal CustomPrincipal principal
  ) {
    if (bindingResult.hasErrors()) {
      return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
    }
    adoptionInfoService.createInfo(dto, UUID.fromString(uid));
    return ResponseEntity.ok().body("Adoption info created successfully");
  }

  @GetMapping("/")
  public Page<AdoptionInfoResponseDto> getAll(int page, int size) {
    return adoptionInfoService.getAll(page, size);
  }


  @GetMapping("/search")
  public Page<AdoptionInfoResponseDto> search(@RequestBody @Valid AdoptionInfoSearchFilterDto dto, int page, int size) {
    return adoptionInfoService.search(dto, page, size);
  }
}
