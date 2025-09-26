package com.sns.marigold.user.repository;

import com.sns.marigold.user.entity.InstitutionUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionUserRepository extends JpaRepository<InstitutionUser, UUID> {

  Optional<InstitutionUser> findByUsername(String username);

  List<InstitutionUser> findByCompanyName(String email);
}
