package com.sns.marigold.chat.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.dto.NewChatDto;
import com.sns.marigold.chat.enums.ChatRoomType;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;
import com.sns.marigold.global.dto.ApiResponse;
import io.hypersistence.tsid.TSID;
import jakarta.validation.groups.Default;
import java.util.List;
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

@RestController
@RequestMapping(UrlConstants.CHAT_BASE + "/rooms") // HTTP REST API
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatService chatService;
  private final SimpMessagingTemplate messagingTemplate;

  @PreAuthorize("isAuthenticated()")
  @PostMapping("")
  public ResponseEntity<ApiResponse<ChatRoomDto>> getOrCreateChatRoom(
      @AuthenticationPrincipal CustomPrincipal principal,
      @RequestBody @Validated({Default.class}) NewChatDto newChatDto) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getOrCreateChatRoom(
                Objects.requireNonNull(principal.getUserId()),
                Objects.requireNonNull(newChatDto.getReceiverId()),
                Objects.requireNonNull(newChatDto.getAdoptionPostId()))));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("")
  public ResponseEntity<ApiResponse<Page<ChatRoomDto>>> getMyRooms(
      @RequestParam(name = "type", required = false) String type,
      @AuthenticationPrincipal CustomPrincipal principal,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) @NonNull
          Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getUserRooms(
                Objects.requireNonNull(principal.getUserId()),
                Objects.requireNonNull(ChatRoomType.fromString(type)),
                pageable)));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}")
  public ResponseEntity<ApiResponse<ChatRoomDto>> getChatRoom(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType @NonNull Long roomId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getChatRoom(roomId, Objects.requireNonNull(principal.getUserId()))));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}/messages")
  public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getRoomMessages(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType @NonNull Long roomId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getRoomMessages(roomId, Objects.requireNonNull(principal.getUserId()))));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}/attachments/{attachmentId}/download-url")
  public ResponseEntity<ApiResponse<String>> getAttachmentDownloadUrl(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType @NonNull Long roomId,
      @PathVariable("attachmentId") @TsidType @NonNull Long attachmentId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getAttachmentDownloadUrl(
                roomId, attachmentId, Objects.requireNonNull(principal.getUserId()))));
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(value = "/{roomId}/messages/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ChatMessageDto>> createFileMessage(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType @NonNull Long roomId,
      @RequestPart(value = "message", required = false) String message,
      @RequestPart("files") List<MultipartFile> files) {
    ChatMessageDto savedMessage =
        chatService.saveFileMessage(
            roomId, message, files, Objects.requireNonNull(principal.getUserId()));
    messagingTemplate.convertAndSend(
        "/sub/chat/room/" + TSID.from(savedMessage.getRoomId()).toString(), savedMessage);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(HttpStatus.CREATED, "created successfully", savedMessage));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{roomId}/leave")
  public ResponseEntity<ApiResponse<Void>> leaveRoom(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType @NonNull Long roomId) {
    chatService.leaveRoom(roomId, Objects.requireNonNull(principal.getUserId()));
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "deleted successfully"));
  }
}
