package com.sns.marigold.chat.repository;

import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  List<ChatMessage> findAllByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);

  // For pagination (later if needed)
  List<ChatMessage> findAllByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);
}
