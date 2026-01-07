package com.sns.marigold.adoption.entity;

import com.sns.marigold.adoption.enums.AdoptionStatus;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

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
@Builder
@AllArgsConstructor
public class AdoptionInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "writer_id", nullable = false)
  private User writer;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime modifiedAt;

  @Enumerated(EnumType.STRING)
  private Species species;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer age;

  @Enumerated(EnumType.STRING)
  private Sex sex;

  @Column(nullable = false)
  private String area;

  @Column(nullable = false)
  private Double weight;

  @Enumerated(EnumType.STRING)
  private Neutering neutering;

  @Lob
  @Column(nullable = false)
  private String features;

  // 입양 상태

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AdoptionStatus status = AdoptionStatus.RECRUITING; // 기본값: 분양중

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "adopter_id") // nullable = true (기본값). 입양 전엔 null이어야 하므로 nullable = true
  private User adopter;

  @Column(nullable = true)
  private LocalDateTime adoptedAt;

  // 이미지

  // 1:N 관계 설정
  // cascade = CascadeType.ALL: 게시글 저장/삭제 시 이미지도 같이 저장/삭제
  // orphanRemoval = true: 리스트에서 이미지를 제거하면 DB에서도 삭제됨
  @OneToMany(mappedBy = "adoptionInfo", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default // 빌더 패턴 사용 시에도 초기화 값 new ArrayList<> 유지
  private List<AdoptionImage> images = new ArrayList<>();

  // --- [연관 관계 편의 메서드] ---
  // 게시글에 이미지를 추가할 때 양방향 관계를 안전하게 맺어주는 메서드
  public void addImage(AdoptionImage image) {
      this.images.add(image);
      image.setAdoptionInfo(this);
  }

  // 입양 완료 처리
  public void completeAdoption(User adopter) {
    this.status = AdoptionStatus.COMPLETED;
    this.adopter = adopter;
    this.adoptedAt = LocalDateTime.now();
}

  // 입양 취소 처리 (필요시)
  public void cancelAdoption() {
      this.status = AdoptionStatus.RECRUITING;
      this.adopter = null;
      this.adoptedAt = null;
  }
}
