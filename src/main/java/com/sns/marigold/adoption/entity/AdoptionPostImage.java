package com.sns.marigold.adoption.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.sns.marigold.storage.entity.StorageFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "adoption_post_image",
    // 같은 업로드 토큰을 두 번 써서 파일 하나를 여러 행이 공유하지 못하게 한다
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_adoption_post_image_stored_file",
            columnNames = "storage_file_id"))
public class AdoptionPostImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "storage_file_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_adoption_post_image_storage_file"))
  private StorageFile storageFile;

  // FK
  // AdoptionPost의 mappedBy="adoptionPost"와 이름이 일치해야 함
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adoption_post_id", nullable = false)
  private AdoptionPost adoptionPost;

  public String getStoredFileName() {
    return storageFile.getStoredFileName();
  }

  public String getOriginalFileName() {
    return storageFile.getOriginalFileName();
  }

  public void setAdoptionPost(AdoptionPost adoptionPost) {
    this.adoptionPost = adoptionPost;
  }
}
