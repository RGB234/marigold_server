package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionAdopter;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdoptionAdopterRepository extends JpaRepository<AdoptionAdopter, Long> {
  Optional<AdoptionAdopter> findByAdoptionPostId(Long adoptionPostId);

  void deleteByAdoptionPostId(Long adoptionPostId);
}
