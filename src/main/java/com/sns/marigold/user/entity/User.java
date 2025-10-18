package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role") // 자식 타입 구분 컬럼
@Table(name = "users")
public abstract class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID) // Hibernate 6.x에서 UUID 생성 전략 지정
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id; // 👈 타입은 UUID로 변경

  public abstract Role getRole();
}
