package com.sns.marigold.adoption.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "adoption_post_image")
public class AdoptionPostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    private LocalDateTime createdAt;

    // S3에 저장된 실제 파일명 (삭제 시 필요)
    @Column(nullable = false)
    private String storedFileName;

    // 사용자가 올린 원본 파일명 (다운로드용)
    @Column(nullable = false)
    private String originalFileName;

    // FK
    // AdoptionPost의 mappedBy="adoptionPost"와 이름이 일치해야 함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adoption_post_id") 
    private AdoptionPost adoptionPost;

    public void setAdoptionPost(AdoptionPost adoptionPost) {
        this.adoptionPost = adoptionPost;
    }
}