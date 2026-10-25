package com.sns.marigold.chat.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.exception.AdoptionPostException;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.dto.ChatAttachmentDto;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatMessageRequestDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatMessageAttachment;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.chat.enums.ChatRoomType;
import com.sns.marigold.chat.exception.ChatException;
import com.sns.marigold.chat.repository.ChatMessageAttachmentRepository;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.StorageFileService;
import com.sns.marigold.storage.service.StorageService;
import com.sns.marigold.storage.service.StorageUploadService;
import com.sns.marigold.storage.service.UploadPurpose;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserException;
import com.sns.marigold.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatMessageAttachmentRepository chatMessageAttachmentRepository;
  private final UserRepository userRepository;
  private final AdoptionPostRepository adoptionPostRepository;
  private final RoomParticipantRepository participantRepository;
  private final StorageService storageService;
  private final StorageUploadService storageUploadService;
  private final StorageFileService storageFileService;
  private final TransactionTemplate transactionTemplate;

  public ChatRoomDto getChatRoom(@NonNull Long roomId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    validateActiveParticipant(chatRoom, currentUserId);
    return convertToRoomDto(Objects.requireNonNull(chatRoom));
  }

  @Transactional
  public ChatRoomDto getOrCreateChatRoom(
      @NonNull Long user1Id, @NonNull Long user2Id, @NonNull Long postId) {
    User user1 = userRepository.findById(user1Id).orElseThrow(UserException::forUserNotFound);
    User user2 = userRepository.findById(user2Id).orElseThrow(UserException::forUserNotFound);
    AdoptionPost adoptionPost =
        adoptionPostRepository
            .findById(postId)
            .orElseThrow(AdoptionPostException::forAdoptionPostNotExists);

    ChatRoom chatRoom =
        chatRoomRepository
            .findByUsersAndAdoptionPost(user1, user2, adoptionPost)
            .orElseGet(
                () -> {
                  // 없으면 생성
                  ChatRoom newRoom = ChatRoom.create(adoptionPost);
                  return chatRoomRepository.save(Objects.requireNonNull(newRoom));
                });

    ensureParticipant(chatRoom, user1);
    ensureParticipant(chatRoom, user2);

    return convertToRoomDto(Objects.requireNonNull(chatRoom));
  }

  /*
   * 채팅방을 종료한다.
   * 이전의 대화내용은 볼 수 있지만 새로 메시지를 보낼 수 없는 상태가 된다.
   */
  @Transactional
  public void closeChatRoom(@NonNull Long roomId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);

    User user = userRepository.findById(currentUserId).orElseThrow(UserException::forUserNotFound);

    // 권한 체크
    participantRepository
        .findByChatRoomAndUser(chatRoom, user)
        .orElseThrow(AuthException::forAccessDenied);

    chatRoom.close();
  }

  @Transactional
  public void closeAllChatRoomsByPostId(@NonNull Long postId) {
    chatRoomRepository.closeAllByAdoptionPostId(postId);
  }

  @Transactional
  public void closeAllChatRoomsByUserId(@NonNull Long userId) {
    chatRoomRepository.closeAllActiveByUserId(userId);
  }

  private void ensureParticipant(ChatRoom chatRoom, User user) {
    participantRepository
        .findByChatRoomAndUser(chatRoom, user)
        .ifPresentOrElse(
            RoomParticipant::reJoin,
            () ->
                participantRepository.save(
                    Objects.requireNonNull(
                        RoomParticipant.builder().chatRoom(chatRoom).user(user).build())));
  }

  public Page<ChatRoomDto> getUserRooms(
      @NonNull Long userId, @NonNull ChatRoomType type, @NonNull Pageable pageable) {
    User user = userRepository.findById(userId).orElseThrow(UserException::forUserNotFound);

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
    };
  }

  public List<ChatMessageDto> getRoomMessages(@NonNull Long roomId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    validateActiveParticipant(chatRoom, currentUserId);
    return chatMessageRepository.findAllByChatRoomOrderByCreatedAtAsc(chatRoom).stream()
        .map(this::convertToMessageDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public ChatMessageDto saveMessage(ChatMessageRequestDto messageDto, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(Objects.requireNonNull(messageDto.getRoomId()));
    User sender = findSender(currentUserId);
    validateCanSendMessage(chatRoom, sender);

    ChatMessage chatMessage =
        ChatMessage.builder()
            .chatRoom(chatRoom)
            .sender(sender)
            .message(Objects.requireNonNullElse(messageDto.getMessage(), ""))
            .build();

    chatMessageRepository.save(Objects.requireNonNull(chatMessage));

    return convertToMessageDto(chatMessage);
  }

  // 업로드 확정 동안 DB 커넥션을 점유하지 않도록 클래스 기본 트랜잭션을 끄고 TransactionTemplate 구간으로 한정
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public ChatMessageDto saveFileMessage(
      @NonNull Long roomId,
      String message,
      List<String> uploadTokens,
      @NonNull Long currentUserId) {
    // 권한 없는 요청에는 저장소 조회 없이 먼저 거부
    transactionTemplate.executeWithoutResult(
        status -> validateCanSendMessage(findChatRoom(roomId), findSender(currentUserId)));

    List<String> tokens = Objects.requireNonNullElse(uploadTokens, List.of());
    String normalizedMessage = Objects.requireNonNullElse(message, "");
    // 메시지도 파일도 없는 경우
    if (!StringUtils.hasText(normalizedMessage) && tokens.isEmpty()) {
      throw ChatException.forEmptyMessage();
    }

    // 저장이 실패해도 파일 행은 PENDING으로 남고 고아 정리가 지운다
    List<FileUploadDto> confirmedFiles =
        storageUploadService.confirm(currentUserId, UploadPurpose.CHAT_ATTACHMENT, tokens);
    return transactionTemplate.execute(
        status -> {
          // 확정 중 채팅방이 종료되었을 수 있으므로 트랜잭션 안에서 다시 검사
          ChatRoom chatRoom = findChatRoom(roomId);
          User sender = findSender(currentUserId);
          validateCanSendMessage(chatRoom, sender);

          ChatMessage chatMessage =
              ChatMessage.builder()
                  .chatRoom(chatRoom)
                  .sender(sender)
                  .message(normalizedMessage)
                  .build();

          for (StorageFile storageFile : storageFileService.attach(confirmedFiles)) {
            chatMessage.addAttachment(
                ChatMessageAttachment.builder().storageFile(storageFile).build());
          }

          ChatMessage savedMessage = chatMessageRepository.saveAndFlush(chatMessage);
          return convertToMessageDto(savedMessage);
        });
  }

  public String getAttachmentDownloadUrl(
      @NonNull Long roomId, @NonNull Long attachmentId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    validateActiveParticipant(chatRoom, currentUserId);

    ChatMessageAttachment attachment =
        chatMessageAttachmentRepository
            .findByIdAndChatMessage_ChatRoom(attachmentId, chatRoom)
            .orElseThrow(StorageException::forFileNotFound);

    return storageService.getDownloadUrl(
        attachment.getStoredFileName(), attachment.getOriginalFileName());
  }

  @Transactional
  public void leaveRoom(@NonNull Long roomId, @NonNull Long userId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    User user = userRepository.findById(userId).orElseThrow(UserException::forUserNotFound);

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
        .status(chatRoom.getStatus())
        .build();
  }

  private ChatRoom findChatRoom(@NonNull Long roomId) {
    return chatRoomRepository.findById(roomId).orElseThrow(ChatException::forRoomNotFound);
  }

  private void validateActiveParticipant(@NonNull ChatRoom chatRoom, @NonNull Long userId) {
    if (!participantRepository.existsByChatRoom_IdAndUser_IdAndLeavedAtIsNull(
        chatRoom.getId(), userId)) {
      throw AuthException.forAccessDenied();
    }
  }

  private User findSender(@NonNull Long currentUserId) {
    return userRepository.findById(currentUserId).orElseThrow(UserException::forUserNotFound);
  }

  private void validateCanSendMessage(@NonNull ChatRoom chatRoom, @NonNull User sender) {
    validateActiveParticipant(chatRoom, Objects.requireNonNull(sender.getId()));

    if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
      throw ChatException.forClosedRoom();
    }
  }

  private ChatMessageDto convertToMessageDto(ChatMessage message) {
    List<ChatAttachmentDto> attachments =
        getAttachments(message).stream()
            .map(
                attachment ->
                    ChatAttachmentDto.builder()
                        .id(attachment.getId())
                        .originalFileName(attachment.getOriginalFileName())
                        .contentType(attachment.getContentType())
                        .fileSize(attachment.getFileSize())
                        .viewUrl(storageService.getViewUrlOrNull(attachment.getStoredFileName()))
                        .build())
            .collect(Collectors.toList());

    return ChatMessageDto.builder()
        .roomId(message.getChatRoom().getId())
        .senderId(message.getSender().getId())
        .senderNickname(message.getSender().getDisplayNickname())
        .senderImageUrl(
            message.getSender().getImage() != null
                ? storageService.getViewUrlOrNull(
                    message.getSender().getImage().getStoredFileName())
                : null)
        .message(message.getMessage())
        .messageType(attachments.isEmpty() ? "TEXT" : "FILE")
        .attachments(attachments)
        .createdAt(message.getCreatedAt())
        .build();
  }

  private List<ChatMessageAttachment> getAttachments(ChatMessage message) {
    return message.getAttachments() == null ? Collections.emptyList() : message.getAttachments();
  }
}
