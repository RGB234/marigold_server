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
      WHERE ((cr.user1 = :user1 AND cr.user2 = :user2) 
      OR (cr.user1 = :user2 AND cr.user2 = :user1)) 
      AND cr.adoptionPost = :adoptionPost
      """)
  Optional<ChatRoom> findByUsersAndAdoptionPost(
      @Param("user1") User user1, 
      @Param("user2") User user2, 
      @Param("adoptionPost") AdoptionPost adoptionPost);

  @Query(
      """
      SELECT cr FROM ChatRoom cr 
      WHERE cr.user1 = :user OR cr.user2 = :user 
      ORDER BY cr.createdAt DESC
      """)
  Page<ChatRoom> findAllByUser(@Param("user") User user, Pageable pageable);

  @Query(
      """
      SELECT cr FROM ChatRoom cr 
      JOIN RoomParticipant rp ON cr = rp.chatRoom 
      WHERE rp.user = :user AND rp.isDeleted = false 
      ORDER BY cr.createdAt DESC
      """)
  Page<ChatRoom> findAllActiveByUser(@Param("user") User user, Pageable pageable);

  @Query("SELECT cr FROM ChatRoom cr WHERE cr.adoptionPost.id = :postId")
  List<ChatRoom> findAllByAdoptionPostId(@Param("postId") Long postId);

  @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.adoptionPost.id = :postId")
  Integer countByAdoptionPostId(@Param("postId") Long postId);
}
