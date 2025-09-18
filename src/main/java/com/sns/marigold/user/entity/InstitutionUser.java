package com.sns.marigold.user.entity;

import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.global.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "institution_users")
@PrimaryKeyJoinColumn(name = "user_id")
@DiscriminatorValue("ROLE_INSTITUTION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstitutionUser extends User {

  @Column(length = 20, unique = true, nullable = false)
  private String username;

  @Email
  @Column(unique = true)
  private String email;

  private String password;

  private String contactPerson; // 담당자명

  private String contactPhone;

  @Column(unique = true)
  private String registrationNumber;

  private String address;

  public void updateUsername(String username) {
    this.username = username;
  }

  public void updatePassword(String encodedPassword) {
    this.password = encodedPassword;
  }

  public void updateProfile(String email, String contactPerson, String contactPhone,
    String registrationNumber, String address) {
    this.email = email;
    this.contactPerson = contactPerson;
    this.contactPhone = contactPhone;
    this.registrationNumber = registrationNumber;
    this.address = address;
  }


  @Builder
  InstitutionUser(Long id, String username,
    String email, String password, String contactPerson, String contactPhone,
    String registrationNumber, String address
  ) {
    super(Role.ROLE_INSTITUTION);
    this.username = username;
    this.email = email;
    this.password = password;
    this.contactPerson = contactPerson;
    this.contactPhone = contactPhone;
    this.registrationNumber = registrationNumber;
    this.address = address;
  }
}
