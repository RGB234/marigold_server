package com.sns.marigold.adoption.entity;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "adoption_image")
public class AdoptionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // S3에 저장된 전체 접근 URL (프론트엔드 이미지 렌더링용)
    @Column(nullable = false, length = 2048)
    private String imageUrl;

    // S3에 저장된 실제 파일명 (삭제 시 필요)
    @Column(nullable = false)
    private String storeFileName;

    // 사용자가 올린 원본 파일명 (다운로드용)
    @Column(nullable = false)
    private String originalFileName;

    // 연관 관계 주인 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adoption_info_id")
    private AdoptionInfo adoptionInfo;

    // 연관 관계 편의 메서드용 세터 (AdoptionInfo 연결)
    public void setAdoptionInfo(AdoptionInfo adoptionInfo) {
        this.adoptionInfo = adoptionInfo;
    }
}