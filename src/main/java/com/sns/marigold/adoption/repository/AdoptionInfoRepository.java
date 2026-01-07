package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionInfo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdoptionInfoRepository extends JpaRepository<AdoptionInfo, Long>,
  JpaSpecificationExecutor<AdoptionInfo> {

  Page<AdoptionInfo> findAllByWriterId(Long writerId, Pageable pageable);
}
