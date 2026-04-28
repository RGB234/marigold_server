package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionAdopter;
import com.sns.marigold.adoption.entity.AdoptionPost;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionAdopterRepository extends JpaRepository<AdoptionAdopter, Long> {
  Optional<AdoptionAdopter> findByAdoptionPostId(Long adoptionPostId);

  @Query("SELECT aa.adoptionPost FROM AdoptionAdopter aa WHERE aa.adopter.id = :adopterId")
  // @Query("SELECT aa.adoptionPost FROM AdoptionAdopter aa WHERE aa.adopter.id = :adopterId AND
  // aa.adoptionPost.deletedAt IS NULL")
  Page<AdoptionPost> findAdoptionPostsByAdopterId(
      @Param("adopterId") Long adopterId, Pageable pageable);

  void deleteByAdoptionPostId(Long adoptionPostId);

  boolean existsByAdoptionPostIdAndAdopterId(Long adoptionPostId, Long adopterId);
}
