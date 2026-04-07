package com.sns.marigold.chat.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.dto.NewChatDto;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;
import com.sns.marigold.global.dto.ApiResponse;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlConstants.CHAT_BASE + "/rooms")
@RequiredArgsConstructor
public class ChatRoomController { // HTTP REST API

  private final ChatService chatService;

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
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK,
            "fetched successfully",
            chatService.getUserRooms(
                Objects.requireNonNull(principal.getUserId()), type, pageable)));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}")
  public ResponseEntity<ApiResponse<ChatRoomDto>> getChatRoom(
      @PathVariable("roomId") @TsidType Long roomId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK, "fetched successfully", chatService.getChatRoom(roomId)));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{roomId}/messages")
  public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getRoomMessages(
      @PathVariable("roomId") @TsidType Long roomId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            HttpStatus.OK, "fetched successfully", chatService.getRoomMessages(roomId)));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/{roomId}/leave")
  public ResponseEntity<ApiResponse<Void>> leaveRoom(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType Long roomId) {
    chatService.leaveRoom(roomId, Objects.requireNonNull(principal.getUserId()));
    return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "deleted successfully"));
  }
}
