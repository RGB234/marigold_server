package com.sns.marigold.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

import io.hypersistence.utils.hibernate.id.Tsid;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "providerInfo", "providerId" })
})
@Builder
@AllArgsConstructor
public class User {
  @Id
  @Tsid // TSID는 toString()을 하면 "0C7X..." 같은 짧은 문자열(Crockford Base32)로 변환됨
  @GeneratedValue(strategy = GenerationType.UUID) // Hibernate 6.x에서 UUID 생성 전략 지정
  @Column(name = "id", updatable = false, nullable = false)
  private Long id; // DB에서는 BIGINT로 저장

  // 비공개 정보

  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @Column(nullable = true)
  private String providerId; // 소셜로그인 계정 id

  @Enumerated(EnumType.STRING) // DB에 숫자가 아닌 문자열(ROLE_PERSON)로 저장
  @Column(nullable = false)
  @Builder.Default // 빌더로 생성 시 값을 안 넣으면 기본값 적용
  private Role role = Role.ROLE_PERSON;

  // 공개 정보

  @Column(length = 50, nullable = false, unique = false) // 12자 이하 + #DEL_TSID -> 12 + 5 + 18 = 35
  private String nickname;

  /**
   * [변경점]
   * 1. orphanRemoval = true 추가: image를 null로 바꾸거나 다른 걸로 교체하면 기존 이미지는 DB에서 자동 삭제
   * 2. CascadeType.ALL: User 저장 시 Image도 자동 저장
   */
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "image_id", nullable = true)
  private UserImage image;

  @Column(nullable = true)
  private LocalDateTime deletedAt; // soft delete timestamp

  public void saveImage(UserImage image) {
    this.image = image; // nullable
  }

  public void deleteImage() {
    this.image = null;
  }

  // 비즈니스 로직
  public void update(String nickname, UserImage newImage) {
    if (nickname != null) {
      this.nickname = nickname;
    }
    if (newImage != null) {
      saveImage(newImage);
    }else{
      deleteImage();
    }
  }

  public void softDelete(){
    this.nickname = this.nickname+"#deleted";
    this.image = null;
    this.providerInfo = null;
    this.providerId = null;
    // this.role = null;
    this.deletedAt = LocalDateTime.now();
  }
}
