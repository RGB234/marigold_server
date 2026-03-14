package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionInfoCreateDto;
import com.sns.marigold.adoption.dto.AdoptionInfoDetailDto;
import com.sns.marigold.adoption.dto.AdoptionInfoDto;
import com.sns.marigold.adoption.dto.AdoptionInfoSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionInfoUpdateDto;
import com.sns.marigold.adoption.dto.AdoptionWithChatDto;
import com.sns.marigold.adoption.enums.AdoptionStatus;
import com.sns.marigold.adoption.service.AdoptionInfoService;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;
import com.sns.marigold.global.dto.ApiResponse;
import jakarta.validation.groups.Default;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlConstants.ADOPTION_BASE)
@RequiredArgsConstructor
@Validated
public class AdoptionInfoController {


  private final AdoptionInfoService adoptionInfoService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Map<String, Object>>> create(
      @ModelAttribute @Validated({Default.class}) AdoptionInfoCreateDto dto,
      @AuthenticationPrincipal CustomPrincipal principal) {
    // Defensive Coding
    if (principal == null) {
      throw AuthException.forUnauthorized();
    }
    Long userId = Objects.requireNonNull(principal.getUserId());
    Long adoptionInfoId = adoptionInfoService.create(dto, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(
        ApiResponse.success(HttpStatus.CREATED, "Adoption info created successfully",
            Map.of("id", adoptionInfoId)));
  }

  /*
  Pageable을 사용하면 ?page=0&size=10&sort=id,desc 처럼 정렬도 자동 지원
  @PageableDefault로 기본값 설정 가능
  @ModelAttribute: 쿼리 파라미터가 없어도 빈 DTO 객체 생성
  */
  @PreAuthorize("permitAll()")
  @GetMapping("")
  public ResponseEntity<ApiResponse<Page<AdoptionInfoDto>>> search(
      @ModelAttribute AdoptionInfoSearchFilterDto dto,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
    Page<AdoptionInfoDto> result = adoptionInfoService.search(dto, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption info search successfully", result));
  }

  @PreAuthorize("permitAll()")
  @GetMapping("/writer/{userId}")
  public ResponseEntity<ApiResponse<Page<AdoptionInfoDto>>> searchByWriter(
      @PathVariable("userId") @TsidType Long userId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
    Page<AdoptionInfoDto> result = adoptionInfoService.searchByWriter(userId, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption info search by writer successfully",
            result));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/chat")
  public ResponseEntity<ApiResponse<Page<AdoptionWithChatDto>>> searchByJoinedChats(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {
    Long uid = principal.getUserId();

    Page<AdoptionWithChatDto> result = adoptionInfoService.searchByJoinedChats(uid, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "SUCCESS", result));
  }

  @PreAuthorize("permitAll()")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AdoptionInfoDetailDto>> getDetail(
      @PathVariable("id") @NonNull Long id) {
    AdoptionInfoDetailDto result = adoptionInfoService.getDetail(id);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption info detail successfully", result));
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> update(
      @NonNull @AuthenticationPrincipal CustomPrincipal principal,
      @NonNull @PathVariable("id") Long id,
      @Validated({Default.class}) @ModelAttribute AdoptionInfoUpdateDto dto) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    Objects.requireNonNull(dto, "dto cannot be null");
    adoptionInfoService.update(id, userId, dto);

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption info updated successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<?>> updateStatus(
      @NonNull @AuthenticationPrincipal CustomPrincipal principal,
      @NonNull @PathVariable("id") Long id,
      @NonNull @RequestParam("status") AdoptionStatus status) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionInfoService.updateStatus(id, status, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption status updated successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> delete(
      @NonNull @AuthenticationPrincipal CustomPrincipal principal,
      @NonNull @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionInfoService.delete(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption info deleted successfully"));
  }
}