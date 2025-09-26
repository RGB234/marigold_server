package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AdoptionInfoRepository extends JpaRepository<AdoptionInfo, Long>,
  JpaSpecificationExecutor<AdoptionInfo> {

  @Query("SELECT a FROM AdoptionInfo a join fetch a.writer WHERE a.id = :id")
  AdoptionInfo findByIdWithWriter_Id(UUID uid);

  List<AdoptionInfo> findAllByWriter_Id(UUID writerId);
}
