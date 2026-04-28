package com.sns.marigold.adoption.entity;

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
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "adoption_comment_image")
public class AdoptionCommentImage {

  @Id
  @Tsid
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @CreatedDate private LocalDateTime createdAt;

  @Column(nullable = false)
  private String storedFileName;

  @Column(nullable = false)
  private String originalFileName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "adoption_comment_id")
  private AdoptionComment adoptionComment;

  public void setAdoptionComment(AdoptionComment adoptionComment) {
    this.adoptionComment = adoptionComment;
  }
}
