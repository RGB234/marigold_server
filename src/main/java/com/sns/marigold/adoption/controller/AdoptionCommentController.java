package com.sns.marigold.adoption.controller;

import com.sns.marigold.adoption.dto.AdoptionCommentCreateDto;
import com.sns.marigold.adoption.dto.AdoptionCommentResponseDto;
import com.sns.marigold.adoption.service.AdoptionCommentService;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.dto.ApiResponse;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlConstants.ADOPTION_BASE + "/{postId}/comments")
@RequiredArgsConstructor
@Validated
public class AdoptionCommentController {

  private final AdoptionCommentService adoptionCommentService;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<Map<String, Object>>> createComment(
      @PathVariable("postId") Long postId,
      @ModelAttribute @Validated({Default.class}) AdoptionCommentCreateDto dto,
      @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    Long commentId = adoptionCommentService.createComment(postId, userId, dto);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.success(
                HttpStatus.CREATED, "Comment created successfully", Map.of("id", commentId)));
  }

  @PreAuthorize("permitAll()")
  @GetMapping("")
  public ResponseEntity<ApiResponse<List<AdoptionCommentResponseDto>>> getComments(
      @PathVariable("postId") Long postId) {
    List<AdoptionCommentResponseDto> result = adoptionCommentService.getComments(postId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Comments fetched successfully", result));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{commentId}")
  public ResponseEntity<ApiResponse<?>> deleteComment(
      @PathVariable("postId") Long postId,
      @PathVariable("commentId") Long commentId,
      @AuthenticationPrincipal CustomPrincipal principal) {
    Long userId = principal.getUserId();
    if (userId == null) {
      throw AuthException.forUnauthorized();
    }

    adoptionCommentService.deleteComment(commentId, userId);
    return ResponseEntity.status(HttpStatus.OK)
        .body(ApiResponse.success(HttpStatus.OK, "Comment deleted successfully"));
  }
}
