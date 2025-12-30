package com.sns.marigold.user.repository;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    List<User> findPersonalUsersByNickname(String nickname);

    Optional<User> findByProviderInfoAndProviderId(ProviderInfo providerInfo, String providerId);
    
    boolean existsByNickname(String nickname);
}
