package com.sns.marigold.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;

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
  @GeneratedValue(strategy = GenerationType.UUID) // Hibernate 6.x에서 UUID 생성 전략 지정
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id; // 👈 타입은 UUID로 변경

  // 비공개 정보

  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @Column(nullable = false)
  private String providerId; // 소셜로그인 계정 id\

  // 공개 정보

  @Column(length = 12, nullable = false, unique = true)
  private String nickname;

  /**
   * [변경점]
   * 1. orphanRemoval = true 추가: image를 null로 바꾸거나 다른 걸로 교체하면 기존 이미지는 DB에서 자동 삭제
   * 2. CascadeType.ALL: User 저장 시 Image도 자동 저장
   */
  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "image_id", nullable = true)
  private UserImage image;

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
}
