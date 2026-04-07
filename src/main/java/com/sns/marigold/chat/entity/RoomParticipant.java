package com.sns.marigold.chat.entity;

import com.sns.marigold.user.entity.User;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "room_participants",
    indexes = {
      // 1. 복합 인덱스: (chat_room_id, user_id) > 특정 채팅방 내의 유저 검색 성능 향상
      @Index(name = "idx_room_user", columnList = "chat_room_id, user_id"),
      // 2. 단일 인덱스: (user_id) > 특정 유저가 참여한 채팅방 검색 성능 향상
      @Index(name = "idx_user", columnList = "user_id")
    })
public class RoomParticipant {

  @Id
  @Tsid
  @Column(updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @CreatedDate
  @Column(updatable = false)
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
