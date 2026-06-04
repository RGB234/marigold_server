package com.sns.marigold.adoption.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springdoc.core.annotations.ParameterObject;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.adoption.dto.AdoptionCandidateDto;
import com.sns.marigold.adoption.dto.AdoptionPostCreateDto;
import com.sns.marigold.adoption.dto.AdoptionPostDetailDto;
import com.sns.marigold.adoption.dto.AdoptionPostDto;
import com.sns.marigold.adoption.dto.AdoptionPostSearchFilterDto;
import com.sns.marigold.adoption.dto.AdoptionPostUpdateDto;
import com.sns.marigold.adoption.dto.CompleteAdoptionRequestDto;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.service.AdoptionPostService;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;
import com.sns.marigold.global.config.SwaggerConfig;
import com.sns.marigold.global.dto.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;

@Tag(name = "Adoption Post", description = "입양 게시글 API")
@RestController
@RequestMapping(UrlConstants.ADOPTION_BASE)
@RequiredArgsConstructor
@Validated
public class AdoptionPostController {

  private final AdoptionPostService adoptionPostService;

  @Operation(
      summary = "입양 게시글 생성",
      description = "입양 게시글과 이미지를 multipart/form-data로 생성합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "생성 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 또는 이미지 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "CSRF token 누락 또는 불일치")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<Map<String, Object>>> create(
      @Parameter(description = "생성할 입양 게시글 정보") @ModelAttribute @Validated({Default.class})
          AdoptionPostCreateDto dto,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal) {
    // Defensive Coding
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    Long adoptionPostId = adoptionPostService.create(dto, userId);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResult.success(
                HttpStatus.CREATED,
                "Adoption post created successfully",
                Map.of("id", adoptionPostId)));
  }

  /*
  Pageable을 사용하면 ?page=0&size=10&sort=id,desc 처럼 정렬도 자동 지원
  @PageableDefault로 기본값 설정 가능
  @ModelAttribute: 쿼리 파라미터가 없어도 빈 DTO 객체 생성
  */
  @Operation(summary = "입양 게시글 검색", description = "검색 조건과 페이지 조건으로 입양 게시글 목록을 조회합니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공")})
  @PreAuthorize("permitAll()")
  @GetMapping("")
  public ResponseEntity<ApiResult<Page<AdoptionPostDto>>> search(
      @ParameterObject @ModelAttribute AdoptionPostSearchFilterDto dto,
      @ParameterObject
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AdoptionPostDto> result = adoptionPostService.search(dto, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption post search successfully", result));
  }

  @Operation(summary = "작성자별 입양 게시글 조회", description = "특정 사용자가 작성한 입양 게시글 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "사용자 ID 형식 오류")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("/writer/{userId}")
  public ResponseEntity<ApiResult<Page<AdoptionPostDto>>> searchByWriter(
      @Parameter(description = "TSID 형식 사용자 ID", required = true) @PathVariable("userId") @TsidType
          Long userId,
      @ParameterObject
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Page<AdoptionPostDto> result = adoptionPostService.searchByWriter(userId, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ApiResult.success(
                HttpStatus.OK, "Adoption post search by writer successfully", result));
  }

  @Operation(
      summary = "입양 신청자별 게시글 조회",
      description = "인증된 사용자가 입양 신청자로 참여한 게시글 목록을 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "사용자 ID 형식 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "본인 외 사용자 조회")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/adopter/{userId}")
  public ResponseEntity<ApiResult<Page<AdoptionPostDto>>> searchByAdopter(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "TSID 형식 사용자 ID", required = true) @PathVariable("userId") @TsidType
          Long userId,
      @ParameterObject
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    Long loggedInUserId = principal.getUserId();
    if (loggedInUserId == null || !loggedInUserId.equals(userId)) {
      throw AuthException.forAccessDenied();
    }

    Page<AdoptionPostDto> result = adoptionPostService.searchByAdopter(userId, pageable);
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ApiResult.success(
                HttpStatus.OK, "Adoption post search by adopter successfully", result));
  }

  @Operation(summary = "입양 게시글 요약 조회", description = "입양 게시글의 목록용 요약 정보를 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("/{id}/summary")
  public ResponseEntity<ApiResult<AdoptionPostDto>> getSummary(
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id) {
    AdoptionPostDto result = adoptionPostService.getSummary(id);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption post successfully", result));
  }

  @Operation(summary = "입양 게시글 상세 조회", description = "입양 게시글의 상세 정보를 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("/{id}")
  public ResponseEntity<ApiResult<AdoptionPostDetailDto>> getDetail(
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id) {
    AdoptionPostDetailDto result = adoptionPostService.getDetail(id);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption post detail successfully", result));
  }

  @Operation(
      summary = "입양 게시글 수정",
      description = "작성자가 입양 게시글과 이미지를 수정합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "수정 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 또는 이미지 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}")
  public ResponseEntity<ApiResult<?>> update(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id,
      @Parameter(description = "수정할 입양 게시글 정보") @Validated({Default.class}) @ModelAttribute
          AdoptionPostUpdateDto dto) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    Objects.requireNonNull(dto, "dto cannot be null");
    adoptionPostService.update(id, userId, dto);

    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption post updated successfully"));
  }

  @Operation(
      summary = "입양 게시글 상태 변경",
      description = "작성자가 입양 게시글 상태를 변경합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
    @ApiResponse(responseCode = "400", description = "상태값 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PatchMapping("/{id}/status")
  public ResponseEntity<ApiResult<?>> updateStatus(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id,
      @Parameter(description = "변경할 게시글 상태", required = true) @RequestParam("status")
          AdoptionPostStatus status) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.updateStatus(id, status, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption status updated successfully"));
  }

  @Operation(
      summary = "입양 게시글 삭제",
      description = "작성자가 입양 게시글을 삭제합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "삭제 성공"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResult<?>> delete(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.delete(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption post deleted successfully"));
  }

  @Operation(
      summary = "입양 후보자 조회",
      description = "작성자가 해당 게시글의 입양 후보자 목록을 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}/candidates")
  public ResponseEntity<ApiResult<List<AdoptionCandidateDto>>> getCandidates(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    List<AdoptionCandidateDto> candidates = adoptionPostService.getCandidates(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ApiResult.success(
                HttpStatus.OK, "Adoption candidates fetched successfully", candidates));
  }

  @Operation(
      summary = "입양 완료 처리",
      description = "작성자가 후보자를 선택해 입양 완료 상태로 변경합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "입양 완료 성공"),
    @ApiResponse(responseCode = "400", description = "요청값 또는 상태 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "게시글 또는 후보자 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/complete")
  public ResponseEntity<ApiResult<?>> completeAdoption(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id,
      @RequestBody @Validated CompleteAdoptionRequestDto request) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.completeAdoption(id, request.getAdopterId(), userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption completed successfully"));
  }

  @Operation(
      summary = "입양 완료 취소",
      description = "작성자가 입양 완료 상태를 취소합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "입양 완료 취소 성공"),
    @ApiResponse(responseCode = "400", description = "상태 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/{id}/cancel-complete")
  public ResponseEntity<ApiResult<?>> cancelAdoption(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("id") Long id) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }
    adoptionPostService.cancelAdoption(id, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Adoption canceled successfully"));
  }
}
