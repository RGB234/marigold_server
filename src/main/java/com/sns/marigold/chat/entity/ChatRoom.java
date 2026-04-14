package com.sns.marigold.chat.entity;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.chat.enums.ChatRoomStatus;
import com.sns.marigold.user.entity.User;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_rooms")
public class ChatRoom {

  @Id
  @Tsid
  @Column(updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adoption_post_id", nullable = false)
  private AdoptionPost adoptionPost;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

  public void close() {
    this.status = ChatRoomStatus.CLOSED;
  }

  public static ChatRoom create(User user1, User user2, AdoptionPost adoptionPost) {
    // Ensure user1.id < user2.id for consistent room identification
    if (user1.getId() > user2.getId()) {
      User temp = user1;
      user1 = user2;
      user2 = temp;
    }
    return ChatRoom.builder()
        // .user1(user1)
        // .user2(user2)
        .adoptionPost(adoptionPost)
        .build();
  }
}
