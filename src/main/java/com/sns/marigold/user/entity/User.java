package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.Role;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Getter
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role") // 자식 타입 구분 컬럼
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class User {

  @Id
  @GeneratedValue
  @UuidGenerator
  protected UUID id;

  //  @Column(nullable = false)
//  @Column(insertable = false, updatable = false)
//  @Enumerated(EnumType.STRING)
//  protected Role role;
  abstract Role getRole();
}
