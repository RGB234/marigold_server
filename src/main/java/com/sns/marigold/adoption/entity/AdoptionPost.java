package com.sns.marigold.adoption.entity;

import com.sns.marigold.adoption.enums.AdoptionPostStatus;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@DynamicUpdate // 변경된 필드만 UPDATE 쿼리 생성
public class AdoptionPost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
  @Column(name = "id", updatable = false, nullable = false)
  private Long id;

  @CreatedDate private LocalDateTime createdAt;

  @LastModifiedDate private LocalDateTime modifiedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "writer_id", nullable = false)
  private User writer;

  @Enumerated(EnumType.STRING)
  private Species species;

  @Enumerated(EnumType.STRING)
  private Sex sex;

  @Enumerated(EnumType.STRING)
  private Neutering neutering;

  @Column(nullable = false)
  private String area;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String features;

  // 선택 입력

  @Column() private Integer age;

  @Column() private Double weight;

  // 입양 상태
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AdoptionPostStatus status;

  // 이미지

  // 1:N 관계 설정
  // cascade = CascadeType.ALL: 게시글 저장/삭제 시 이미지도 같이 저장/삭제
  // orphanRemoval = true: 리스트에서 이미지를 제거하면 DB에서도 삭제됨
  @OneToMany(
      mappedBy = "adoptionPost",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<AdoptionPostImage> images;

  @Builder
  public AdoptionPost(
      User writer,
      Species species,
      String title,
      Integer age,
      Sex sex,
      String area,
      Double weight,
      Neutering neutering,
      List<AdoptionPostImage> images,
      String features) {
    this.writer = writer;
    this.species = species;
    this.title = title;
    this.age = age;
    this.sex = sex;
    this.area = area;
    this.weight = weight;
    this.neutering = neutering;
    this.features = features;
    //
    this.images = (images != null) ? images : new ArrayList<>();
    this.status = AdoptionPostStatus.PROCEEDING;
  }

  // Null로 수정 가능
  public void updateInfo(AdoptionPostEditor editor) {
    this.title = editor.getTitle();
    this.age = editor.getAge();
    this.weight = editor.getWeight();
    this.features = editor.getFeatures();
    this.area = editor.getArea();
    this.species = editor.getSpecies();
    this.sex = editor.getSex();
    this.neutering = editor.getNeutering();
  }

  // // 입양 완료 처리
  // public void completeAdoption(User adopter) {
  //   this.status = AdoptionStatus.COMPLETED;
  //   this.adopter = adopter;
  //   this.adoptedAt = LocalDateTime.now();
  // }

  // 입양 상태 변경 (예약 등)
  public void updateStatus(AdoptionPostStatus status) {
    this.status = status;
  }

  // // 입양 취소 처리 (필요시)
  // public void cancelAdoption() {
  //   this.status = AdoptionStatus.PROCEEDING;
  //   this.adopter = null;
  //   this.adoptedAt = null;
  // }

  // --- [연관 관계 편의 메서드 수정] ---
  public void addImage(AdoptionPostImage image) {
    this.images.add(image);
    // [중요] 자식 엔티티에도 부모(나 자신)를 세팅해줘야 FK가 들어감
    if (image.getAdoptionPost() != this) {
      image.setAdoptionPost(this);
    }
  }

  // ==========================================================
  // 이미지 수정 (효율적인 교체 전략)
  // ==========================================================
  public void replaceImages(List<String> remainingImageNames, List<AdoptionPostImage> newImages) {
    // 1. 삭제할 이미지 제거 (리스트에서 제거하면 orphanRemoval=true에 의해 DB 삭제됨)
    // remainingImageNames에 포함되지 않은 기존 이미지는 삭제
    this.images.removeIf(image -> !remainingImageNames.contains(image.getStoredFileName()));

    // 2. 새로운 이미지 추가
    if (newImages != null) {
      for (AdoptionPostImage image : newImages) {
        this.addImage(image);
      }
    }
  }

  // ==========================================================
  // 이미지 수정 (전체 교체 전략)
  // ==========================================================
  public void changeImages(List<AdoptionPostImage> newImages) {
    // 1. 기존 이미지 리스트를 비움 (orphanRemoval=true 덕분에 DB에서 DELETE 쿼리 발생)
    this.images.clear();

    // 2. 새로운 이미지 추가 (CascadeType.ALL 덕분에 DB에 INSERT 쿼리 발생)
    if (newImages != null && !newImages.isEmpty()) {
      for (AdoptionPostImage image : newImages) {
        this.addImage(image); // 편의 메서드 재사용 (양방향 연관관계 설정)
      }
    }
  }
}
