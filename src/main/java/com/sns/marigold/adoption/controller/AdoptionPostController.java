package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionPostCreateDto;
import com.sns.marigold.adoption.dto.AdoptionPostDetailDto;
import com.sns.marigold.adoption.dto.AdoptionPostDto;
import com.sns.marigold.adoption.dto.AdoptionPostSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionPostUpdateDto;
import com.sns.marigold.adoption.dto.AdoptionPostWithChatDto;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.service.AdoptionPostService;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;
import com.sns.marigold.global.dto.ApiResponse;
import jakarta.validation.groups.Default;
import com.sns.marigold.adoption.dto.CompleteAdoptionRequestDto;
import com.sns.marigold.adoption.dto.AdoptionCandidateDto;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlConstants.ADOPTION_BASE)
@RequiredArgsConstructor
@Validated
public class AdoptionPostController {

  private final AdoptionPostService adoptionPostService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Map<String, Object>>> create(
      @ModelAttribute @Validated({Default.class}) AdoptionPostCreateDto dto,
      @AuthenticationPrincipal CustomPrincipal principal) {
    // Defensive Coding
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    Long adoptionPostId = adoptionPostService.create(dto, userId);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                HttpStatus.CREATED,
                "Adoption post created successfully",
                Map.of("id", adoptionPostId)));
  }

  /*
  Pageable을 사용하면 ?page=0&size=10&sort=id,desc 처럼 정렬도 자동 지원
  @PageableDefault로 기본값 설정 가능
  @ModelAttribute: 쿼리 파라미터가 없어도 빈 DTO 객체 생성
  */
  @PreAuthorize("permitAll()")
  @GetMapping("")
  public ResponseEntity<ApiResponse<Page<AdoptionPostDto>>> search(
      @ModelAttribute AdoptionPostSearchFilterDto dto,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AdoptionPostDto> result = adoptionPostService.search(dto, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption post search successfully", result));
  }

  @PreAuthorize("permitAll()")
  @GetMapping("/writer/{userId}")
  public ResponseEntity<ApiResponse<Page<AdoptionPostDto>>> searchByWriter(
      @PathVariable("userId") @TsidType Long userId,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AdoptionPostDto> result = adoptionPostService.searchByWriter(userId, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ApiResponse.success(
                HttpStatus.OK, "Adoption post search by writer successfully", result));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/chat")
  public ResponseEntity<ApiResponse<Page<AdoptionPostWithChatDto>>> searchByJoinedChats(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Long uid = principal.getUserId();
    if (uid == null) {
      throw AuthException.forUnauthorized();
    }

    Page<AdoptionPostWithChatDto> result = adoptionPostService.searchByJoinedChats(uid, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "SUCCESS", result));
  }

  @PreAuthorize("permitAll()")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<AdoptionPostDetailDto>> getDetail(@PathVariable("id") Long id) {
    AdoptionPostDetailDto result = adoptionPostService.getDetail(id);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption post detail successfully", result));
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> update(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("id") Long id,
      @Validated({Default.class}) @ModelAttribute AdoptionPostUpdateDto dto) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    Objects.requireNonNull(dto, "dto cannot be null");
    adoptionPostService.update(id, userId, dto);

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption post updated successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResponse<?>> updateStatus(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("id") Long id,
      @RequestParam("status") AdoptionPostStatus status) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.updateStatus(id, status, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption status updated successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<?>> delete(
      @AuthenticationPrincipal CustomPrincipal principal, @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.delete(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption post deleted successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}/candidates")
  public ResponseEntity<ApiResponse<List<AdoptionCandidateDto>>> getCandidates(
      @AuthenticationPrincipal CustomPrincipal principal, @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    List<AdoptionCandidateDto> candidates = adoptionPostService.getCandidates(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption candidates fetched successfully", candidates));
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/complete")
  public ResponseEntity<ApiResponse<?>> completeAdoption(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("id") Long id,
      @RequestBody @Validated CompleteAdoptionRequestDto request) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.completeAdoption(id, request.getAdopterId(), userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption completed successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/cancel-complete")
  public ResponseEntity<ApiResponse<?>> cancelAdoption(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.cancelAdoption(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Adoption canceled successfully"));
  }
}
