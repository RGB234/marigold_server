package com.sns.marigold.chat.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.dto.NewChatDto;
import com.sns.marigold.chat.service.ChatService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.annotation.TsidType;

import io.hypersistence.tsid.TSID;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UrlConstants.CHAT_BASE)
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatService chatService;

  // WebSocket에도 ApiResponse를 쓰는 것은 과함
  // 1. 페이로드가 무거움
  // 2. 이미 STOMP에는 에러가 발생하면 ERROR 프레임을 보낼 수 있는 매커니즘이 있음

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/rooms")
  public ResponseEntity<ChatRoomDto> createRoom(
      @AuthenticationPrincipal CustomPrincipal principal,
      @RequestBody @Validated({Default.class}) NewChatDto newChatDto) {
    return ResponseEntity.ok(
        chatService.createRoom(
            Objects.requireNonNull(principal.getUserId()),
            Objects.requireNonNull(newChatDto.getReceiverId()),
            Objects.requireNonNull(newChatDto.getAdoptionPostId())));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/rooms")
  public ResponseEntity<Page<ChatRoomDto>> getMyRooms(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        chatService.getUserRooms(Objects.requireNonNull(principal.getUserId()), pageable));
  }

  // DEBUGGING
  // @PreAuthorize("isAuthenticated()")
  // @GetMapping("/rooms/{roomId}/messages")
  // public ResponseEntity<List<ChatMessageDto>> getRoomMessages(
  //     @PathVariable("roomId") @TsidType String roomId) {
  //   Long longRoomId = TSID.from(roomId).toLong();
  //   return ResponseEntity.ok(chatService.getRoomMessages(longRoomId));
  // }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/rooms/{roomId}/messages")
  public ResponseEntity<List<ChatMessageDto>> getRoomMessages(
        @PathVariable("roomId") @TsidType Long roomId) {
    return ResponseEntity.ok(chatService.getRoomMessages(roomId));
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping("/rooms/{roomId}")
  public ResponseEntity<Void> deleteRoom(
      @AuthenticationPrincipal CustomPrincipal principal,
      @PathVariable("roomId") @TsidType Long roomId) {
    chatService.leaveRoom(roomId, Objects.requireNonNull(principal.getUserId()));
    return ResponseEntity.ok().build();
  }
}
