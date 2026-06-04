package com.sns.marigold.chat.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Table(name = "chat_message_attachments")
public class ChatMessageAttachment {

  @Id
  @Tsid
  @Column(updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "chat_message_id", nullable = false)
  private ChatMessage chatMessage;

  @Column(nullable = false)
  private String storedFileName;

  @Column(nullable = false)
  private String originalFileName;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private long fileSize;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  public void setChatMessage(ChatMessage chatMessage) {
    this.chatMessage = chatMessage;
  }
}
