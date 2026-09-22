package com.sns.marigold.chat.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sns.marigold.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "room_participants",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_room_participant_room_user",
          columnNames = {"chat_room_id", "user_id"})
    },
    indexes = {
      // 특정 유저가 참여한 채팅방 검색 성능 향상
      @Index(name = "idx_user", columnList = "user_id")
    })
public class RoomParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  private LocalDateTime leavedAt;

  // @Builder.Default private boolean isExited = false;

  public void leave() {
    // this.isExited = true;
    this.leavedAt = LocalDateTime.now();
  }

  public void reJoin() {
    // this.isExited = false;
    this.leavedAt = null;
  }
}
