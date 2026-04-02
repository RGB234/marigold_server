package com.sns.marigold.chat.entity;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.user.entity.User;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "chat_rooms",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"user1_id", "user2_id", "adoption_post_id"})})
public class ChatRoom {

  @Id
  @Tsid
  @Column(updatable = false, nullable = false)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adoption_post_id", nullable = false)
  private AdoptionPost adoptionPost;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user1_id", nullable = false)
  private User user1;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user2_id", nullable = false)
  private User user2;

  @CreatedDate
  @Column(updatable = false)
  private LocalDateTime createdAt;

  public static ChatRoom create(User user1, User user2, AdoptionPost adoptionPost) {
    // Ensure user1.id < user2.id for consistent room identification
    if (user1.getId() > user2.getId()) {
      User temp = user1;
      user1 = user2;
      user2 = temp;
    }
    return ChatRoom.builder().user1(user1).user2(user2).adoptionPost(adoptionPost).build();
  }
}
