package com.sns.marigold.chat.service;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.enums.ChatRoomType;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
  private final S3Service storageService;

  public ChatRoomDto getChatRoom(Long roomId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
    return convertToRoomDto(Objects.requireNonNull(chatRoom));
  }

  @Transactional
  public ChatRoomDto getOrCreateChatRoom(Long user1Id, Long user2Id, Long postId) {
    User user1 = userRepository.getReferenceById(user1Id);
    User user2 = userRepository.getReferenceById(user2Id);
    AdoptionPost adoptionPost = adoptionPostRepository.getReferenceById(postId);

    ChatRoom chatRoom =
        chatRoomRepository
            .findByUsersAndAdoptionPost(user1, user2, adoptionPost)
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

  /*
    채팅방(1:1)의 참여자 중 하나가 채팅방을 종료한다.
    이전의 대화내용은 볼 수 있지만 새로 메시지를 보낼 수 없는 상태가 된다.
  */
  public void closeChatRoom(Long roomId, Long currentUserId) {
    ChatRoom chatRoom =
        chatRoomRepository
            .findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
    User user =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUserId));

    // 권한 체크
    participantRepository
        .findByChatRoomAndUser(chatRoom, user)
        .orElseThrow(() -> AuthException.forAccessDenied());

    chatRoom.close();
    chatRoomRepository.save(chatRoom);
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

  public Page<ChatRoomDto> getUserRooms(Long userId, ChatRoomType type, Pageable pageable) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    return switch (type) {
      case WRITER ->
          chatRoomRepository
              .findAllActiveByUserAsWriter(user, pageable)
              .map(this::convertToRoomDto);
      case INQUIRER ->
          chatRoomRepository
              .findAllActiveByUserAsInquirer(user, pageable)
              .map(this::convertToRoomDto);
      case ALL ->
          chatRoomRepository.findAllActiveByUser(user, pageable).map(this::convertToRoomDto);
      default -> throw new IllegalArgumentException("Invalid chat room type: " + type);
    };
  }

  public List<ChatMessageDto> getRoomMessages(Long roomId) {
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

    if (chatRoom.getStatus() == com.sns.marigold.chat.enums.ChatRoomStatus.CLOSED) {
      throw new IllegalStateException("종료된 채팅방에는 메시지를 보낼 수 없습니다.");
    }

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
    List<RoomParticipant> participants = participantRepository.findAllByChatRoom(chatRoom);
    for (RoomParticipant participant : participants) {
      ensureParticipant(chatRoom, participant.getUser());
    }

    return convertToMessageDto(chatMessage);
  }

  @Transactional
  public void leaveRoom(Long roomId, Long userId) {
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

  private ChatRoomDto convertToRoomDto(ChatRoom chatRoom) {
    List<RoomParticipant> participants = participantRepository.findAllByChatRoom(chatRoom);
    if (participants.size() != 2) {
      throw new IllegalStateException(
          "1:1 채팅방은 참가자 2명이어야 합니다. roomId=" + chatRoom.getId() + ", count=" + participants.size());
    }
    List<User> orderedUsers =
        participants.stream()
            .map(RoomParticipant::getUser)
            .sorted(Comparator.comparing(User::getId))
            .toList();
    User user1 = orderedUsers.get(0);
    User user2 = orderedUsers.get(1);

    boolean isPostDeleted = chatRoom.getAdoptionPost().getDeletedAt() != null;
    String postTitle = isPostDeleted ? "삭제된 게시글입니다" : chatRoom.getAdoptionPost().getTitle();

    return ChatRoomDto.builder()
        .id(chatRoom.getId())
        .postId(chatRoom.getAdoptionPost().getId())
        .postTitle(postTitle)
        .postWriterId(chatRoom.getAdoptionPost().getWriter().getId())
        .user1Id(user1.getId())
        .user1Nickname(user1.getDisplayNickname())
        .user2Id(user2.getId())
        .user2Nickname(user2.getDisplayNickname())
        .createdAt(chatRoom.getCreatedAt())
        .status(chatRoom.getStatus().name())
        .build();
  }

  private ChatMessageDto convertToMessageDto(ChatMessage message) {
    return ChatMessageDto.builder()
        .roomId(message.getChatRoom().getId())
        .senderId(message.getSender().getId())
        .senderNickname(message.getSender().getDisplayNickname())
        .senderImageUrl(
            message.getSender().getImage() != null
                ? storageService.getPresignedGetObject(
                    message.getSender().getImage().getStoredFileName())
                : null)
        .message(message.getMessage())
        .createdAt(message.getCreatedAt())
        .build();
  }
}
