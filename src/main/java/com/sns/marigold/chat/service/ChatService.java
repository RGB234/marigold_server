package com.sns.marigold.chat.service;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.repository.AdoptionPostRepository;
import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.chat.dto.ChatAttachmentDto;
import com.sns.marigold.chat.dto.ChatMessageDto;
import com.sns.marigold.chat.dto.ChatRoomDto;
import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatMessageAttachment;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.chat.enums.ChatRoomType;
import com.sns.marigold.chat.repository.ChatMessageAttachmentRepository;
import com.sns.marigold.chat.repository.ChatMessageRepository;
import com.sns.marigold.chat.repository.ChatRoomRepository;
import com.sns.marigold.chat.repository.RoomParticipantRepository;
import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.S3Service;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

  private static final int MAX_CHAT_ATTACHMENT_COUNT = ValidationPolicy.ChatAttachment.MAX_COUNT;
  private static final long MAX_CHAT_ATTACHMENT_SIZE =
      ValidationPolicy.ChatAttachment.MAX_FILE_SIZE_BYTES;
  private static final long MAX_CHAT_ATTACHMENT_TOTAL_SIZE =
      ValidationPolicy.ChatAttachment.MAX_TOTAL_SIZE_BYTES;
  private static final Map<String, List<String>> ALLOWED_CHAT_ATTACHMENT_MIME_TYPES =
      ValidationPolicy.ChatAttachment.ALLOWED_MIME_TYPES_BY_EXTENSION;

  private final ChatRoomRepository chatRoomRepository;
  private final ChatMessageRepository chatMessageRepository;
  private final ChatMessageAttachmentRepository chatMessageAttachmentRepository;
  private final UserRepository userRepository;
  private final AdoptionPostRepository adoptionPostRepository;
  private final RoomParticipantRepository participantRepository;
  private final S3Service storageService;

  public ChatRoomDto getChatRoom(@NonNull Long roomId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    validateParticipant(chatRoom, currentUserId);
    return convertToRoomDto(Objects.requireNonNull(chatRoom));
  }

  @Transactional
  public ChatRoomDto getOrCreateChatRoom(
      @NonNull Long user1Id, @NonNull Long user2Id, @NonNull Long postId) {
    User user1 = userRepository.getReferenceById(user1Id);
    User user2 = userRepository.getReferenceById(user2Id);
    AdoptionPost adoptionPost = adoptionPostRepository.getReferenceById(postId);

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
  public void closeChatRoom(@NonNull Long roomId, @NonNull Long currentUserId) {
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
        .orElseThrow(AuthException::forAccessDenied);

    chatRoom.close();
    chatRoomRepository.save(chatRoom);
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

  public List<ChatMessageDto> getRoomMessages(@NonNull Long roomId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    validateParticipant(chatRoom, currentUserId);
    return chatMessageRepository.findAllByChatRoomOrderByCreatedAtAsc(chatRoom).stream()
        .map(this::convertToMessageDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public ChatMessageDto saveMessage(ChatMessageDto messageDto, @NonNull Long currentUserId) {
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
    reactivateParticipants(chatRoom);

    return convertToMessageDto(chatMessage);
  }

  @Transactional
  public ChatMessageDto saveFileMessage(
      @NonNull Long roomId,
      String message,
      List<MultipartFile> files,
      @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    User sender = findSender(currentUserId);
    validateCanSendMessage(chatRoom, sender);

    List<MultipartFile> validFiles = validateChatAttachmentFiles(files);
    String normalizedMessage = Objects.requireNonNullElse(message, "");
    if (!StringUtils.hasText(normalizedMessage) && validFiles.isEmpty()) {
      throw StorageException.forFileInvalid("message and files are empty");
    }

    List<FileUploadDto> uploadedFiles = storageService.uploadFilesToS3(validFiles);
    try {
      ChatMessage chatMessage =
          ChatMessage.builder()
              .chatRoom(chatRoom)
              .sender(sender)
              .message(normalizedMessage)
              .build();

      for (FileUploadDto uploadedFile : uploadedFiles) {
        chatMessage.addAttachment(
            ChatMessageAttachment.builder()
                .storedFileName(uploadedFile.getStoredFileName())
                .originalFileName(uploadedFile.getOriginalFileName())
                .contentType(uploadedFile.getContentType())
                .fileSize(uploadedFile.getFileSize())
                .build());
      }

      ChatMessage savedMessage = chatMessageRepository.saveAndFlush(chatMessage);
      reactivateParticipants(chatRoom);
      return convertToMessageDto(savedMessage);
    } catch (RuntimeException e) {
      storageService.deleteUploadedFilesFromS3(uploadedFiles);
      throw e;
    }
  }

  public String getAttachmentDownloadUrl(
      @NonNull Long roomId, @NonNull Long attachmentId, @NonNull Long currentUserId) {
    ChatRoom chatRoom = findChatRoom(roomId);
    validateParticipant(chatRoom, currentUserId);

    ChatMessageAttachment attachment =
        chatMessageAttachmentRepository
            .findByIdAndChatMessage_ChatRoom(attachmentId, chatRoom)
            .orElseThrow(StorageException::forFileNotFound);

    return storageService.getPresignedDownloadObject(
        attachment.getStoredFileName(), attachment.getOriginalFileName());
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

  private ChatRoom findChatRoom(@NonNull Long roomId) {
    return chatRoomRepository
        .findById(roomId)
        .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));
  }

  private void validateParticipant(@NonNull ChatRoom chatRoom, @NonNull Long userId) {
    if (!participantRepository.existsByChatRoom_IdAndUser_Id(chatRoom.getId(), userId)) {
      throw AuthException.forAccessDenied();
    }
  }

  private User findSender(@NonNull Long currentUserId) {
    return userRepository
        .findById(currentUserId)
        .orElseThrow(() -> new IllegalArgumentException("Sender not found: " + currentUserId));
  }

  private void validateCanSendMessage(@NonNull ChatRoom chatRoom, @NonNull User sender) {
    participantRepository
        .findByChatRoomAndUser(chatRoom, sender)
        .orElseThrow(AuthException::forAccessDenied);

    if (chatRoom.getStatus() == ChatRoomStatus.CLOSED) {
      throw new IllegalStateException("종료된 채팅방에는 메시지를 보낼 수 없습니다.");
    }
  }

  private void reactivateParticipants(ChatRoom chatRoom) {
    List<RoomParticipant> participants = participantRepository.findAllByChatRoom(chatRoom);
    for (RoomParticipant participant : participants) {
      ensureParticipant(chatRoom, participant.getUser());
    }
  }

  private List<MultipartFile> validateChatAttachmentFiles(List<MultipartFile> files) {
    List<MultipartFile> nonEmptyFiles =
        files == null
            ? Collections.emptyList()
            : files.stream().filter(file -> file != null && !file.isEmpty()).toList();

    if (nonEmptyFiles.isEmpty()) {
      throw StorageException.forFileInvalid("files are empty");
    }
    if (nonEmptyFiles.size() > MAX_CHAT_ATTACHMENT_COUNT) {
      throw StorageException.forFileInvalid("max file count: " + MAX_CHAT_ATTACHMENT_COUNT);
    }

    long totalSize = 0L;
    Tika tika = new Tika();
    for (MultipartFile file : nonEmptyFiles) {
      if (file.getSize() > MAX_CHAT_ATTACHMENT_SIZE) {
        throw StorageException.forFileInvalid(file.getOriginalFilename());
      }
      totalSize += file.getSize();
      if (totalSize > MAX_CHAT_ATTACHMENT_TOTAL_SIZE) {
        throw StorageException.forFileInvalid("max total file size exceeded");
      }

      String extension = getFileExtension(file);
      List<String> allowedMimeTypes = ALLOWED_CHAT_ATTACHMENT_MIME_TYPES.get(extension);
      if (allowedMimeTypes == null) {
        throw StorageException.forFileInvalid(file.getOriginalFilename());
      }

      try (InputStream inputStream = file.getInputStream()) {
        String detectedMimeType = tika.detect(inputStream);
        if (!allowedMimeTypes.contains(detectedMimeType)) {
          throw StorageException.forFileInvalid(file.getOriginalFilename());
        }
      } catch (IOException e) {
        throw StorageException.forFileUploadFailed(file.getOriginalFilename(), e);
      }
    }

    return nonEmptyFiles;
  }

  private String getFileExtension(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    int extensionStart = originalFilename == null ? -1 : originalFilename.lastIndexOf(".");
    if (extensionStart < 0 || extensionStart == originalFilename.length() - 1) {
      throw StorageException.forFileInvalid(originalFilename);
    }
    return originalFilename.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
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
                        .downloadUrl(
                            storageService.getPresignedGetObject(attachment.getStoredFileName()))
                        .build())
            .collect(Collectors.toList());

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
        .messageType(attachments.isEmpty() ? "TEXT" : "FILE")
        .attachments(attachments)
        .createdAt(message.getCreatedAt())
        .build();
  }

  private List<ChatMessageAttachment> getAttachments(ChatMessage message) {
    return message.getAttachments() == null ? Collections.emptyList() : message.getAttachments();
  }
}
