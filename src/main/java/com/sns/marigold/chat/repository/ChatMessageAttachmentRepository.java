package com.sns.marigold.chat.repository;

import com.sns.marigold.chat.entity.ChatMessageAttachment;
import com.sns.marigold.chat.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageAttachmentRepository
    extends JpaRepository<ChatMessageAttachment, Long> {

  Optional<ChatMessageAttachment> findByIdAndChatMessage_ChatRoom(Long id, ChatRoom chatRoom);
}
