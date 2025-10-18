package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.dto.InstitutionUserSecurityUpdateDto;
import com.sns.marigold.user.dto.InstitutionUserUpdateDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Entity
@Getter
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ROLE_INSTITUTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Table(name = "institution_users")
public class InstitutionUser extends User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID) // Hibernate 6.x에서 UUID 생성 전략 지정
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id; // 👈 타입은 UUID로 변경

  @Column(unique = true, nullable = false)
  private String username;
  @Column(nullable = false)
  private String password;

  @Email
  @Column(unique = true, nullable = false)
  private String email;

  @Column(nullable = false)
  private String companyName; // 회사명

  @Column(nullable = false)
  private String repName; // 대표자명

  @Column(unique = true, nullable = false, length = 12) // 000-00-000000
  private String brn; // 사업자등록번호 business registration number

  @Column(nullable = false, length = 5)
  private String zipCode; // 우편번호

  @Column(nullable = false)
  private String address;

  @Column(nullable = false)
  private String detailedAddress;

  public void update(InstitutionUserUpdateDto dto) {
    if (dto.getCompanyName() != null) {
      this.email = dto.getCompanyName();
    }
    if (dto.getRepName() != null) {
      this.repName = dto.getRepName();
    }
    if (dto.getBrn() != null) {
      this.brn = dto.getBrn();
    }
    if (dto.getZipCode() != null) {
      this.zipCode = dto.getZipCode();
    }
    if (dto.getAddress() != null) {
      this.address = dto.getAddress();
    }
    if (dto.getDetailedAddress() != null) {
      this.detailedAddress = dto.getDetailedAddress();
    }
  }

  public void updateSecurityInfo(InstitutionUserSecurityUpdateDto dto,
                                 PasswordEncoder passwordEncoder) {
    if (dto.getUsername() != null) {
      this.username = dto.getUsername();
    }
    if (dto.getPassword() != null) {
      this.password = passwordEncoder.encode(dto.getPassword());
    }
  }

  @Override
  public Role getRole() {
    return Role.ROLE_INSTITUTION;
  }
}
