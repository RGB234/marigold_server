package com.sns.marigold.storage.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 업로드한 파일의 메타데이터와 수명 상태. 도메인 테이블은 이 행을 FK로 참조한다. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
    name = "storage_file",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_storage_file_name", columnNames = "stored_file_name"),
    indexes = {
      // 만료된 PENDING 조회
      @Index(name = "idx_storage_file_status_created", columnList = "status, created_at")
    })
public class StorageFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  // 스토리지의 정식 키. 확정 전 파일은 같은 이름으로 pending/ 아래에 있다
  @Column(nullable = false, updatable = false)
  private String storedFileName;

  // 사용자가 올린 원본 파일명 (다운로드용)
  @Column(nullable = false, updatable = false)
  private String originalFileName;

  @Column(nullable = false, updatable = false)
  private String contentType;

  @Column(nullable = false, updatable = false)
  private long fileSize;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private StorageFileStatus status;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime statusChangedAt;
}
