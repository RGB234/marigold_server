package com.sns.marigold.user.repository;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findPersonalUsersByNickname(String nickname);
    boolean existsByProviderInfoAndProviderId(ProviderInfo providerInfo, String providerId);
    boolean existsByNickname(String nickname);
    Optional<User> findByProviderInfoAndProviderId(ProviderInfo providerInfo, String providerId);
}
