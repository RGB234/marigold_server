package com.sns.marigold.chat.repository;

import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findAllByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

    // For pagination (later if needed)
    List<ChatMessage> findAllByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);
}
