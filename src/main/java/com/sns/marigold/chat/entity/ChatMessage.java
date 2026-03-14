package com.sns.marigold.chat.entity;

import com.sns.marigold.user.entity.User;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
@Table(name = "chat_messages")
public class ChatMessage {

  @Id
  @Tsid
  @Column(updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_room_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String message;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;
}
