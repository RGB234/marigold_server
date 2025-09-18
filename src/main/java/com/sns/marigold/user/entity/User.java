package com.sns.marigold.user.entity;

import com.sns.marigold.global.annotation.Enum;
import com.sns.marigold.global.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role") // 자식 타입 구분 컬럼
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
  protected Long id;

  //  @Column(nullable = false)
  @Column(name = "role", insertable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  @Enum(target = Role.class)
  protected Role role;

  //
//  //  @Builder
  User(Role role) {
    this.role = role;
  }
}
