package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.PersonalUserUpdateDto;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "personal_users",
  uniqueConstraints = {
    @UniqueConstraint(columnNames = {"providerInfo", "providerId"})
  })
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ROLE_PERSON")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class PersonalUser extends User {

  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo; // 소셜로그인 제공 서비스 종류 (Google, Kakao, ...)

  @Column(nullable = false)
  private String providerId; // 소셜로그인 계정 id

  @Column(length = 12, nullable = false)
  private String nickname;

  @Override
  public Role getRole() {
    return Role.ROLE_PERSON;
  }

  public void update(PersonalUserUpdateDto dto) {
    if (dto.getNickname() != null) {
      this.nickname = dto.getNickname();
    }
  }
}
