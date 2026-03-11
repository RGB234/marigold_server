package com.sns.marigold.chat.controller;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatService chatService;

    @PostMapping("/rooms")
    public ResponseEntity<ChatRoomDto> createRoom(@AuthenticationPrincipal CustomPrincipal principal,
                                                 @RequestParam("receiverId") @NonNull Long receiverId) {
        return ResponseEntity.ok(chatService.getOrCreateRoom(Objects.requireNonNull(principal.getUserId()), receiverId));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDto>> getMyRooms(@AuthenticationPrincipal CustomPrincipal principal) {
        return ResponseEntity.ok(chatService.getUserRooms(Objects.requireNonNull(principal.getUserId())));
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getRoomMessages(@NonNull @PathVariable("roomId") Long roomId) {
        return ResponseEntity.ok(chatService.getRoomMessages(roomId));
    }
}
