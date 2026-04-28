package com.sns.marigold.adoption.entity;

import com.sns.marigold.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "adoption_comment")
public class AdoptionComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "adoption_post_id", nullable = false)
  private AdoptionPost adoptionPost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "writer_id", nullable = false)
  private User writer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private AdoptionComment parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AdoptionComment> children = new ArrayList<>();

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  @CreatedDate private LocalDateTime createdAt;

  @LastModifiedDate private LocalDateTime modifiedAt;

  private LocalDateTime deletedAt;

  @OneToMany(
      mappedBy = "adoptionComment",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<AdoptionCommentImage> images = new ArrayList<>();

  @Builder
  public AdoptionComment(
      AdoptionPost adoptionPost, User writer, AdoptionComment parent, String content) {
    this.adoptionPost = adoptionPost;
    this.writer = writer;
    this.parent = parent;
    this.content = content;
    this.images = new ArrayList<>();
    this.children = new ArrayList<>();
  }

  public void addImage(AdoptionCommentImage image) {
    this.images.add(image);
    image.setAdoptionComment(this);
  }

  public void update(String content) {
    this.content = content;
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
  }

  public void changeImages(List<AdoptionCommentImage> newImages) {
    this.images.clear();
    if (newImages != null && !newImages.isEmpty()) {
      for (AdoptionCommentImage image : newImages) {
        this.addImage(image);
      }
    }
  }
}
