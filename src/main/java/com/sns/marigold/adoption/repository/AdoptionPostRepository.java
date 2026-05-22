package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionPostRepository
    extends JpaRepository<AdoptionPost, Long>, JpaSpecificationExecutor<AdoptionPost> {

  Page<AdoptionPost> findByWriter_IdAndDeletedAtIsNull(
      @Param("writerId") Long writerId, Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE AdoptionPost a SET a.deletedAt = CURRENT_TIMESTAMP WHERE a.writer.id = :writerId")
  void setDeletedTimeByWriter(@Param("writerId") Long writerId);
}
