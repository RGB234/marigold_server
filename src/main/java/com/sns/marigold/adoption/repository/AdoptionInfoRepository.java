package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionInfo;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface AdoptionInfoRepository extends JpaRepository<AdoptionInfo, Long>,
  JpaSpecificationExecutor<AdoptionInfo> {

  // @Query("SELECT a FROM AdoptionInfo a WHERE a.writer.id = :writerId") <- 있어도 되고 없어도 됨
  Page<AdoptionInfo> findByWriter_Id(@Param("writerId") UUID writerId, Pageable pageable);
}
