package com.sns.marigold.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sns.marigold.chat.entity.ChatMessage;
import com.sns.marigold.chat.entity.ChatRoom;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

  List<ChatMessage> findAllByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
}
