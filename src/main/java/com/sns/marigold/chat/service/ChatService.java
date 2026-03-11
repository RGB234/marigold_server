package com.sns.marigold.chat.service;

import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatRoomDto getOrCreateRoom(@NonNull Long user1Id, @NonNull Long user2Id) {
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + user2Id));

        ChatRoom chatRoom = chatRoomRepository.findByUsers(user1, user2)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.create(user1, user2);
                    return chatRoomRepository.save(Objects.requireNonNull(newRoom));
                });

        return convertToRoomDto(Objects.requireNonNull(chatRoom));
    }

    public List<ChatRoomDto> getUserRooms(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return chatRoomRepository.findAllByUser(user).stream()
                .map(this::convertToRoomDto)
                .collect(Collectors.toList());
    }

    public List<ChatMessageDto> getRoomMessages(@NonNull Long roomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
        return chatMessageRepository.findAllByChatRoomOrderByCreatedAtAsc(chatRoom).stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageDto saveMessage(ChatMessageDto messageDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(Objects.requireNonNull(messageDto.getRoomId()))
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + messageDto.getRoomId()));
        User sender = userRepository.findById(Objects.requireNonNull(messageDto.getSenderId()))
                .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + messageDto.getSenderId()));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .message(messageDto.getMessage())
                .build();

        chatMessageRepository.save(Objects.requireNonNull(chatMessage));
        return convertToMessageDto(chatMessage);
    }

    private ChatRoomDto convertToRoomDto(@NonNull ChatRoom chatRoom) {
        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .user1Id(chatRoom.getUser1().getId())
                .user1Nickname(chatRoom.getUser1().getNickname())
                .user2Id(chatRoom.getUser2().getId())
                .user2Nickname(chatRoom.getUser2().getNickname())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    private ChatMessageDto convertToMessageDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .roomId(message.getChatRoom().getId())
                .senderId(message.getSender().getId())
                .senderNickname(message.getSender().getNickname())
                .message(message.getMessage())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
