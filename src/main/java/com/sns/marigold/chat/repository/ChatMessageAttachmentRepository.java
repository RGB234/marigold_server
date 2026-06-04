package com.sns.marigold.chat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sns.marigold.chat.entity.ChatMessageAttachment;
import com.sns.marigold.chat.entity.ChatRoom;

public interface ChatMessageAttachmentRepository
    extends JpaRepository<ChatMessageAttachment, Long> {

  Optional<ChatMessageAttachment> findByIdAndChatMessage_ChatRoom(Long id, ChatRoom chatRoom);
}
