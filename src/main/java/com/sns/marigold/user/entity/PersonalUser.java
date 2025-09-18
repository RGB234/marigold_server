package com.sns.marigold.user.entity;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "personal_users")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ROLE_PERSON")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalUser extends User {

  @Column(length = 20, unique = true, nullable = false)
  private String username;

  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  private String providerId; // 소셜로그인 계정 id

  @Builder
  PersonalUser(String username, ProviderInfo providerInfo,
    String providerId) {
    super(Role.ROLE_PERSON);
    this.username = username;
    this.providerInfo = providerInfo;
    this.providerId = providerId;
  }

  public void updateUsername(String username) {
    this.username = username;
  }
}
