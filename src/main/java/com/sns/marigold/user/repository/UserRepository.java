package com.sns.marigold.user.repository;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  List<User> findPersonalUsersByNickname(String nickname);

  boolean existsByProviderInfoAndProviderId(ProviderInfo providerInfo, String providerId);

  boolean existsByNickname(String nickname);

  Optional<User> findByProviderInfoAndProviderId(ProviderInfo providerInfo, String providerId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);
}
