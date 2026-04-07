package com.sns.marigold.user.entity;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
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
@Table(name = "user_image")
public class UserImage {
  @Id
  @Tsid
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @CreatedDate private LocalDateTime createdAt;

  // S3에 저장된 실제 파일명
  @Column(nullable = false)
  private String storedFileName;

  // 사용자가 올린 원본 파일명 (다운로드용)
  @Column(nullable = false)
  private String originalFileName;
}
