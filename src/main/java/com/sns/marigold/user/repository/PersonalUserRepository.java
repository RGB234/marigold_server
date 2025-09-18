package com.sns.marigold.user.repository;

import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.user.entity.PersonalUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalUserRepository extends JpaRepository<PersonalUser, Long> {

  Optional<PersonalUser> findByUsername(String username);

  Optional<PersonalUser> findByProviderInfoAndProviderId(ProviderInfo providerInfo,
    String providerId);

  @Modifying
  @Query("UPDATE PersonalUser u SET " +
    "u.username = COALESCE(:username, u.username) " +
    "where u.id = :id")
  int update(
    @Param("id") Long id,
    @Param("username") String username
  );
}
