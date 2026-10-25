package com.sns.marigold.storage.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.global.config.SwaggerConfig;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.dto.UploadIssueRequestDto;
import com.sns.marigold.storage.dto.UploadIssueResponseDto;
import com.sns.marigold.storage.service.StorageUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Storage", description = "파일 업로드 API")
@RestController
@RequestMapping(UrlConstants.STORAGE_BASE)
@RequiredArgsConstructor
public class StorageUploadController {

  private final StorageUploadService storageUploadService;

  @Operation(
      summary = "업로드 URL 발급",
      description = "파일마다 직접 PUT할 URL과 업로드 토큰을 발급합니다. 클라이언트는 URL로 파일을 올린 뒤, 도메인 API에 토큰을 보냅니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "발급 성공"),
    @ApiResponse(responseCode = "400", description = "개수, 크기, 확장자 정책 위반"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "CSRF token 누락 또는 불일치")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/uploads")
  public List<UploadIssueResponseDto> issueUploads(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @RequestBody @Validated UploadIssueRequestDto request) {
    return storageUploadService.issue(Objects.requireNonNull(principal.getUserId()), request);
  }
}
