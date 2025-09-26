package com.sns.marigold.user.repository;

import com.sns.marigold.user.entity.InstitutionUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionUserRepository extends JpaRepository<InstitutionUser, UUID> {

  Optional<InstitutionUser> findByEmail(String email);

  List<InstitutionUser> findByCompanyName(String email);
}
