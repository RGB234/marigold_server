package com.sns.marigold.chat.repository;

import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

  Optional<RoomParticipant> findByChatRoomAndUser(ChatRoom chatRoom, User user);

  List<RoomParticipant> findAllByChatRoom(ChatRoom chatRoom);

  List<RoomParticipant> findAllByUserAndIsDeletedFalse(User user);
}
