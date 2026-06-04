package com.sns.marigold.chat.controller;

import java.util.List;
import java.util.Objects;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.dto.NewChatDto;
import com.sns.marigold.chat.enums.ChatRoomType;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;
import com.sns.marigold.global.config.SwaggerConfig;
import com.sns.marigold.global.dto.ApiResult;

import io.hypersistence.tsid.TSID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat Room", description = "채팅방과 채팅 메시지 REST API")
@RestController
@RequestMapping(UrlConstants.CHAT_BASE + "/rooms") // HTTP REST API
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatService chatService;
  private final SimpMessagingTemplate messagingTemplate;

  @Operation(
      summary = "채팅방 조회 또는 생성",
      description = "입양 게시글과 상대 사용자 기준으로 기존 채팅방을 조회하거나 새로 생성합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 또는 생성 성공"),
    @ApiResponse(responseCode = "400", description = "요청값 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "CSRF token 누락 또는 불일치"),
    @ApiResponse(responseCode = "404", description = "입양 게시글 또는 사용자 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping("")
  public ResponseEntity<ApiResult<ChatRoomDto>> getOrCreateChatRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @RequestBody @Validated({Default.class}) NewChatDto newChatDto) {
    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getOrCreateChatRoom(
                Objects.requireNonNull(principal.getUserId()),
                Objects.requireNonNull(newChatDto.getReceiverId()),
                Objects.requireNonNull(newChatDto.getAdoptionPostId()))));
  }

  @Operation(
      summary = "내 채팅방 목록 조회",
      description = "인증된 사용자의 채팅방 목록을 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "채팅방 타입 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("")
  public ResponseEntity<ApiResult<Page<ChatRoomDto>>> getMyRooms(
      @Parameter(description = "채팅방 타입", required = false)
          @RequestParam(name = "type", required = false)
          String type,
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @ParameterObject
          @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          @NonNull
          Pageable pageable) {
    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getUserRooms(
                Objects.requireNonNull(principal.getUserId()),
                Objects.requireNonNull(ChatRoomType.fromString(type)),
                pageable)));
  }

  @Operation(
      summary = "채팅방 단건 조회",
      description = "채팅방 ID로 채팅방 정보를 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "채팅방 ID 형식 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "채팅방 참여자 아님"),
    @ApiResponse(responseCode = "404", description = "채팅방 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}")
  public ResponseEntity<ApiResult<ChatRoomDto>> getChatRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "TSID 형식 채팅방 ID", required = true)
          @PathVariable("roomId")
          @TsidType
          @NonNull
          Long roomId) {
    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getChatRoom(roomId, Objects.requireNonNull(principal.getUserId()))));
  }

  @Operation(
      summary = "채팅방 메시지 조회",
      description = "채팅방의 메시지 목록을 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "채팅방 ID 형식 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "채팅방 참여자 아님"),
    @ApiResponse(responseCode = "404", description = "채팅방 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}/messages")
  public ResponseEntity<ApiResult<List<ChatMessageDto>>> getRoomMessages(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "TSID 형식 채팅방 ID", required = true)
          @PathVariable("roomId")
          @TsidType
          @NonNull
          Long roomId) {
    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getRoomMessages(roomId, Objects.requireNonNull(principal.getUserId()))));
  }

  @Operation(
      summary = "첨부파일 다운로드 URL 조회",
      description = "채팅 메시지 첨부파일의 다운로드 URL을 조회합니다.",
      security = {@SecurityRequirement(name = SwaggerConfig.BEARER_AUTH)})
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공"),
    @ApiResponse(responseCode = "400", description = "ID 형식 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "채팅방 참여자 아님"),
    @ApiResponse(responseCode = "404", description = "채팅방 또는 첨부파일 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}/attachments/{attachmentId}/download-url")
  public ResponseEntity<ApiResult<String>> getAttachmentDownloadUrl(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "TSID 형식 채팅방 ID", required = true)
          @PathVariable("roomId")
          @TsidType
          @NonNull
          Long roomId,
      @Parameter(description = "TSID 형식 첨부파일 ID", required = true)
          @PathVariable("attachmentId")
          @TsidType
          @NonNull
          Long attachmentId) {
    return ResponseEntity.ok(
        ApiResult.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getAttachmentDownloadUrl(
                roomId, attachmentId, Objects.requireNonNull(principal.getUserId()))));
  }

  @Operation(
      summary = "파일 메시지 생성",
      description = "채팅방에 파일 첨부 메시지를 multipart/form-data로 생성하고 WebSocket으로 브로드캐스트합니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "생성 성공"),
    @ApiResponse(responseCode = "400", description = "요청값 또는 파일 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "채팅방 참여자 아님 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "채팅방 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "/{roomId}/messages/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResult<ChatMessageDto>> createFileMessage(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "TSID 형식 채팅방 ID", required = true)
          @PathVariable("roomId")
          @TsidType
          @NonNull
          Long roomId,
      @Parameter(description = "메시지 본문", required = false)
          @RequestPart(value = "message", required = false)
          String message,
      @Parameter(description = "첨부파일 목록", required = true) @RequestPart("files")
          List<MultipartFile> files) {
    ChatMessageDto savedMessage =
        chatService.saveFileMessage(
            roomId, message, files, Objects.requireNonNull(principal.getUserId()));
    messagingTemplate.convertAndSend(
        "/sub/chat/room/" + TSID.from(savedMessage.getRoomId()).toString(), savedMessage);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResult.success(HttpStatus.CREATED, "created successfully", savedMessage));
  }

  @Operation(
      summary = "채팅방 나가기",
      description = "인증된 사용자가 채팅방에서 나갑니다.",
      security = {
        @SecurityRequirement(name = SwaggerConfig.BEARER_AUTH),
        @SecurityRequirement(name = SwaggerConfig.CSRF_TOKEN)
      })
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "나가기 성공"),
    @ApiResponse(responseCode = "400", description = "채팅방 ID 형식 오류"),
    @ApiResponse(responseCode = "401", description = "인증 필요"),
    @ApiResponse(responseCode = "403", description = "채팅방 참여자 아님 또는 CSRF token 오류"),
    @ApiResponse(responseCode = "404", description = "채팅방 없음")
  })
  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{roomId}/leave")
  public ResponseEntity<ApiResult<Void>> leaveRoom(
      @Parameter(hidden = true) @AuthenticationPrincipal CustomPrincipal principal,
      @Parameter(description = "TSID 형식 채팅방 ID", required = true)
          @PathVariable("roomId")
          @TsidType
          @NonNull
          Long roomId) {
    chatService.leaveRoom(roomId, Objects.requireNonNull(principal.getUserId()));
    return ResponseEntity.ok(ApiResult.success(HttpStatus.OK, "deleted successfully"));
  }
}
