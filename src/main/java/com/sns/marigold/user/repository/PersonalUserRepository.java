package com.sns.marigold.user.repository;

import com.sns.marigold.global.enums.ProviderInfo;
import com.sns.marigold.user.entity.PersonalUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalUserRepository extends JpaRepository<PersonalUser, UUID> {

  Optional<PersonalUser> findByUsername(String username);

  Optional<PersonalUser> findByProviderInfoAndProviderId(ProviderInfo providerInfo,
    String providerId);
}
