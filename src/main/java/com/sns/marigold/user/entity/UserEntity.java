package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.UserUpdateDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
  @Column(name = "id", unique = true, nullable = false)
  private Long id;

  @NotNull
  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @NotNull
  private String providerId; // 소셜로그인 계정 id

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Role role;

  @Column(length = 20, unique = true, nullable = false)
  private String username;


  public UserEntity() {
  }

  public void updateFrom(UserUpdateDTO updateDto) {
    this.username = updateDto.getUsername();
  }
}
