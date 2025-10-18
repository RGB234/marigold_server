package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.Role;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ROLE_ADMIN")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "admin_users")
public class AdminUser extends User {
  @Column(unique = true, nullable = false)
  private String username;
  @Column(nullable = false)
  private String password;
  @Column(nullable = false)
  private String nickname;


  @Override
  public Role getRole() {
    return Role.ROLE_ADMIN;
  }
}
