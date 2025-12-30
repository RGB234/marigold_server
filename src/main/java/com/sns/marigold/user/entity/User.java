package com.sns.marigold.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.dto.update.UserUpdateDto;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"providerInfo", "providerId"})
    })
@Builder
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID) // Hibernate 6.x에서 UUID 생성 전략 지정
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id; // 👈 타입은 UUID로 변경

  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @Column(nullable = false)
  private String providerId; // 소셜로그인 계정 id

  @Column(length = 12, nullable = false, unique = true)
  private String nickname;

  public void applyUpdate(UserUpdateDto dto) {
    if (dto == null) {
      return;
    }
    if (dto.getNickname() != null) {
      this.nickname = dto.getNickname();
    }
  }
}
