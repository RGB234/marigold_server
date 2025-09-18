package com.sns.marigold.user.repository;

import com.sns.marigold.user.entity.InstitutionUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionUserRepository extends JpaRepository<InstitutionUser, Long> {

  Optional<InstitutionUser> findByUsername(String username);

  Optional<InstitutionUser> findByEmail(String email);


  // null이 아닌 필드만 업데이트
  @Modifying
  @Query("UPDATE InstitutionUser u SET " +
    "u.username = COALESCE(:username, u.username) , " +
    "u.email = COALESCE(:email, u.email), " +
    "u.contactPerson = COALESCE(:contactPerson, u.contactPerson), " +
    "u.contactPhone = COALEASCE(:contactPhone, u.contactPhone), " +
    "u.registrationNumber = COALESCE(:registrationNumber, u.registrationNumber) , " +
    "u.address = COALESCE(:address, u.address) " +
    "where u.id = :id")
  int update(@Param("id") Long id,
    @Param("username") String username,
    @Param("email") String email,
    @Param("contactPerson") String contactPerson,
    @Param("contactPhone") String contactPhone,
    @Param("registrationNumber") String registrationNumber,
    @Param("address") String address
  );
}
