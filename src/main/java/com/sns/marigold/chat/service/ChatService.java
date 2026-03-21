package com.sns.marigold.chat.service;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final AdoptionPostRepository adoptionPostRepository;
  private final RoomParticipantRepository participantRepository;

  public ChatRoomDto getRoom(@NonNull Long roomId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
    return convertToRoomDto(Objects.requireNonNull(chatRoom));
  }

  @Transactional
  public ChatRoomDto createRoom(
      @NonNull Long user1Id, @NonNull Long user2Id, @NonNull Long postId) {
    User user1 =
        userRepository
            .findById(user1Id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + user1Id));
    User user2 =
        userRepository
            .findById(user2Id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + user2Id));
    AdoptionPost adoptionPost =
        adoptionPostRepository
            .findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Adoption post not found: " + postId));

    ChatRoom chatRoom =
        chatRoomRepository
            .findByUsers(user1, user2)
            .orElseGet(
                () -> {
                  // 없으면 생성
                  ChatRoom newRoom = ChatRoom.create(user1, user2, adoptionPost);
                  return chatRoomRepository.save(Objects.requireNonNull(newRoom));
                });

    ensureParticipant(chatRoom, user1);
    ensureParticipant(chatRoom, user2);

    return convertToRoomDto(Objects.requireNonNull(chatRoom));
  }

  private void ensureParticipant(ChatRoom chatRoom, User user) {
    participantRepository
        .findByChatRoomAndUser(chatRoom, user)
        .ifPresentOrElse(
            RoomParticipant::reJoin,
            () ->
                participantRepository.save(
                    RoomParticipant.builder().chatRoom(chatRoom).user(user).build()));
  }

  public Page<ChatRoomDto> getUserRooms(@NonNull Long userId, @NonNull Pageable pageable) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    return chatRoomRepository.findAllActiveByUser(user, pageable).map(this::convertToRoomDto);
  }

  public List<ChatMessageDto> getRoomMessages(@NonNull Long roomId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
    return chatMessageRepository.findAllByChatRoomOrderByCreatedAtAsc(chatRoom).stream()
        .map(this::convertToMessageDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public ChatMessageDto saveMessage(ChatMessageDto messageDto) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(Objects.requireNonNull(messageDto.getRoomId()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Chat room not found: " + messageDto.getRoomId()));
    User sender =
        userRepository
            .findById(Objects.requireNonNull(messageDto.getSenderId()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException("Sender not found: " + messageDto.getSenderId()));

    ChatMessage chatMessage =
        ChatMessage.builder()
            .chatRoom(chatRoom)
            .sender(sender)
            .message(messageDto.getMessage())
            .build();

    chatMessageRepository.save(Objects.requireNonNull(chatMessage));

    // Re-activate room for both participants
    ensureParticipant(chatRoom, chatRoom.getUser1());
    ensureParticipant(chatRoom, chatRoom.getUser2());

    return convertToMessageDto(chatMessage);
  }

  @Transactional
  public void leaveRoom(@NonNull Long roomId, @NonNull Long userId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    participantRepository.findByChatRoomAndUser(chatRoom, user).ifPresent(RoomParticipant::leave);
  }

  private ChatRoomDto convertToRoomDto(@NonNull ChatRoom chatRoom) {
    return ChatRoomDto.builder()
        .id(chatRoom.getId())
        .postId(chatRoom.getAdoptionPost().getId())
        .postTitle(chatRoom.getAdoptionPost().getTitle())
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
