package com.sns.marigold.chat.repository;

import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.chat.entity.RoomParticipant;
import com.sns.marigold.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

  Optional<RoomParticipant> findByChatRoomAndUser(ChatRoom chatRoom, User user);

  List<RoomParticipant> findAllByChatRoom(ChatRoom chatRoom);

  @Query(
      """
      SELECT rp
      FROM RoomParticipant rp
      JOIN rp.chatRoom.adoptionPost ap
      WHERE ap.id = :adoptionPostId
      """)
  List<RoomParticipant> findAllByAdoptionPostId(@Param("adoptionPostId") Long adoptionPostId);
}
