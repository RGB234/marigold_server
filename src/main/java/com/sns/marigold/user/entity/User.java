package com.sns.marigold.user.entity;

import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.enums.UserStatus;
import io.hypersistence.tsid.TSID;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "users",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"providerInfo", "providerId"}),
      @UniqueConstraint(columnNames = {"nickname"})
    })
@Builder
@AllArgsConstructor
public class User {
  @Id
  @Tsid
  @Column(updatable = false, nullable = false)
  private Long id;

  @Enumerated(EnumType.STRING)
  private ProviderInfo providerInfo;

  @Column(nullable = true)
  private String providerId;

  @Column(nullable = true, unique = true)
  private String email;

  @Column(nullable = true)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private Role role = Role.ROLE_PERSON;

  @Column(length = 50, nullable = false, unique = true)
  private String nickname;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "image_id", nullable = true)
  private UserImage image;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @Column(nullable = true)
  private LocalDateTime deletedAt;

  public void saveImage(UserImage image) {
    this.image = image;
  }

  public void deleteImage() {
    this.image = null;
  }

  public void update(String nickname) {
    if (nickname != null) {
      this.nickname = nickname;
    }
  }

  public void update(String nickname, UserImage newImage) {
    if (nickname != null) {
      this.nickname = nickname;
    }
    if (newImage != null) {
      saveImage(newImage);
    } else {
      deleteImage();
    }
  }

  public void addEmailAndPassword(String email, String encodedPassword) {
    this.email = email;
    this.password = encodedPassword;
  }

  public void linkOAuth2(ProviderInfo providerInfo, String providerId) {
    this.providerInfo = providerInfo;
    this.providerId = providerId;
  }

  public boolean hasLocalCredentials() {
    return this.email != null
        && !this.email.isBlank()
        && this.password != null
        && !this.password.isBlank();
  }

  public boolean hasOAuth2Link() {
    return this.providerInfo != null && this.providerId != null && !this.providerId.isBlank();
  }

  public void softDelete() {
    this.nickname = "deleted-user-" + TSID.from(this.id).toString();
    this.image = null;
    this.providerInfo = null;
    this.providerId = null;
    this.status = UserStatus.DELETED;
    this.deletedAt = LocalDateTime.now();
  }

  public void ban() {
    this.status = UserStatus.BANNED;
  }

  public void sleep() {
    this.status = UserStatus.SLEEP;
  }

  public void activate() {
    this.status = UserStatus.ACTIVE;
  }

  public boolean isActive() {
    return this.status == UserStatus.ACTIVE;
  }

  public String getDisplayNickname() {
    if (this.status == UserStatus.DELETED) {
      return "탈퇴한 유저";
    }
    return this.nickname;
  }
}
