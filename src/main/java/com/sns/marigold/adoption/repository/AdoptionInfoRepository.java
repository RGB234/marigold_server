package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AdoptionInfoRepository extends JpaRepository<AdoptionInfo, Long>,
  JpaSpecificationExecutor<AdoptionInfo> {

  AdoptionInfo findById(UUID uid);

  Page<AdoptionInfo> findAllByWriterId(UUID writerId, Pageable pageable);
}
