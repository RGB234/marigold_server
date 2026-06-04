package com.sns.marigold.adoption.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.adoption.dto.AdoptionCommentCreateDto;
import com.sns.marigold.adoption.dto.AdoptionCommentResponseDto;
import com.sns.marigold.adoption.dto.AdoptionCommentUpdateDto;
import com.sns.marigold.adoption.service.AdoptionCommentService;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.UrlConstants;
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

@Tag(name = "Adoption Comment", description = "입양 게시글 댓글 API")
@RestController
@RequestMapping(UrlConstants.ADOPTION_BASE + "/{postId}/comments")
@RequiredArgsConstructor
@Validated
public class AdoptionCommentController {

  private final AdoptionCommentService adoptionCommentService;

  @Operation(
      summary = "댓글 생성",
      description = "입양 게시글에 댓글과 이미지를 multipart/form-data로 생성합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "생성 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 또는 이미지 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "CSRF token 누락 또는 불일치"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<Map<String, Object>>> createComment(
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("postId") Long postId,
      @Parameter(description = "생성할 댓글 정보") @ModelAttribute @Validated({Default.class})
          AdoptionCommentCreateDto dto,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    Long commentId = adoptionCommentService.createComment(postId, userId, dto);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResult.success(
                HttpStatus.CREATED, "Comment created successfully", Map.of("id", commentId)));
  }

  @Operation(
      summary = "댓글 수정",
      description = "작성자가 댓글 내용과 이미지를 multipart/form-data로 수정합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "수정 성공"),
    @ApiResponse(responseCode = "400", description = "입력값 또는 이미지 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "게시글 또는 댓글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<?>> updateComment(
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("postId") Long postId,
      @Parameter(description = "댓글 ID", required = true) @PathVariable("commentId") Long commentId,
      @Parameter(description = "수정할 댓글 정보") @ModelAttribute @Validated({Default.class})
          AdoptionCommentUpdateDto dto,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    adoptionCommentService.updateComment(postId, commentId, userId, dto);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Comment updated successfully"));
  }

  @Operation(summary = "댓글 목록 조회", description = "입양 게시글의 댓글 목록을 조회합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "404", description = "게시글 없음")
  })
  @PreAuthorize("permitAll()")
  @GetMapping("")
  public ResponseEntity<ApiResult<List<AdoptionCommentResponseDto>>> getComments(
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("postId") Long postId) {
    List<AdoptionCommentResponseDto> result = adoptionCommentService.getComments(postId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Comments fetched successfully", result));
  }

  @Operation(
      summary = "댓글 삭제",
      description = "작성자가 댓글을 삭제합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "삭제 성공"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "권한 없음 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "댓글 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<ApiResult<?>> deleteComment(
      @Parameter(description = "입양 게시글 ID", required = true) @PathVariable("postId") Long postId,
      @Parameter(description = "댓글 ID", required = true) @PathVariable("commentId") Long commentId,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    adoptionCommentService.deleteComment(commentId, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResult.success(HttpStatus.OK, "Comment deleted successfully"));
  }
}
