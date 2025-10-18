package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.PersonalUserUpdateDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ROLE_PERSON")
@Table(name = "personal_users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"providerInfo", "providerId"})
    })
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

  public void update(PersonalUserUpdateDto dto) {
    if (dto.getNickname() != null) {
      this.nickname = dto.getNickname();
    }
  }

  @Override
  public Role getRole() {
    return Role.ROLE_PERSON;
  }
}
