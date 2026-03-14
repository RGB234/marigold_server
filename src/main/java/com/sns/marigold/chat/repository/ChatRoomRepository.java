package com.sns.marigold.chat.repository;

import com.sns.marigold.chat.entity.ChatRoom;
import com.sns.marigold.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.user1 = :user1 AND cr.user2 = :user2) OR (cr.user1 = :user2 AND cr.user2 = :user1)")
    Optional<ChatRoom> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.user1 = :user OR cr.user2 = :user ORDER BY cr.createdAt DESC")
    Page<ChatRoom> findAllByUser(@Param("user") User user, Pageable pageable);

}
