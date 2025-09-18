package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoResponseDto;
import com.sns.marigold.adoption.service.AdoptionInfoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

@RestController
@RequestMapping("adoption")
@RequiredArgsConstructor
@Slf4j
public class AdoptionInfoController {

  private final AdoptionInfoService adoptionInfoService;

  @PostMapping("/create")
  public void create(@RequestBody @Valid AdoptionInfoCreateDto dto,
    @SessionAttribute(value = "uid", required = true) String uid // nullable X
  ) {
    adoptionInfoService.createInfo(dto, uid);
  }

  @GetMapping("/all")
  public List<AdoptionInfoResponseDto> getAll() {
    return adoptionInfoService.getAll();
  }
}
