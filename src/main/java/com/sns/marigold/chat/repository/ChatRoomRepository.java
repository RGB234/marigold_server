package com.sns.marigold.chat.repository;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

  @Query(
      """
      SELECT cr FROM ChatRoom cr
      WHERE cr.adoptionPost = :adoptionPost
      AND EXISTS (
          SELECT 1 FROM RoomParticipant rp1
          WHERE rp1.chatRoom = cr AND rp1.user = :user1)
      AND EXISTS (
          SELECT 1 FROM RoomParticipant rp2
          WHERE rp2.chatRoom = cr AND rp2.user = :user2)
      ORDER BY cr.createdAt ASC
      """)
  Optional<ChatRoom> findByUsersAndAdoptionPost(
      @Param("user1") User user1,
      @Param("user2") User user2,
      @Param("adoptionPost") AdoptionPost adoptionPost);

  @Query(
      value =
          """
          SELECT DISTINCT cr FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user
          ORDER BY cr.createdAt DESC
          """,
      countQuery =
          """
          SELECT COUNT(DISTINCT cr) FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user
          """)
  Page<ChatRoom> findAllByUser(@Param("user") User user, Pageable pageable);

  @Query(
      value =
          """
          SELECT DISTINCT cr FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user AND rp.leavedAt IS NULL
          ORDER BY cr.createdAt DESC
          """,
      countQuery =
          """
          SELECT COUNT(DISTINCT cr) FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user AND rp.leavedAt IS NULL
          """)
  Page<ChatRoom> findAllActiveByUser(@Param("user") User user, Pageable pageable);

  @Query(
      value =
          """
          SELECT DISTINCT cr FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user AND rp.leavedAt IS NULL
          AND cr.adoptionPost.writer = :user
          ORDER BY cr.createdAt DESC
          """,
      countQuery =
          """
          SELECT COUNT(DISTINCT cr) FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user AND rp.leavedAt IS NULL
          AND cr.adoptionPost.writer = :user
          """)
  Page<ChatRoom> findAllActiveByUserAsWriter(@Param("user") User user, Pageable pageable);

  @Query(
      value =
          """
          SELECT DISTINCT cr FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user AND rp.leavedAt IS NULL
          AND cr.adoptionPost.writer <> :user
          ORDER BY cr.createdAt DESC
          """,
      countQuery =
          """
          SELECT COUNT(DISTINCT cr) FROM ChatRoom cr
          JOIN RoomParticipant rp ON cr = rp.chatRoom
          WHERE rp.user = :user AND rp.leavedAt IS NULL
          AND cr.adoptionPost.writer <> :user
          """)
  Page<ChatRoom> findAllActiveByUserAsInquirer(@Param("user") User user, Pageable pageable);

  @Query("SELECT cr FROM ChatRoom cr WHERE cr.adoptionPost.id = :postId")
  List<ChatRoom> findAllByAdoptionPostId(@Param("postId") Long postId);

  @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.adoptionPost.id = :postId")
  Integer countByAdoptionPostId(@Param("postId") Long postId);
}
