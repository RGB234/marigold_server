package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionInfo;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionInfoRepository extends JpaRepository<AdoptionInfo, Long>,
    JpaSpecificationExecutor<AdoptionInfo> {

  // @Query("SELECT a FROM AdoptionInfo a WHERE a.writer.id = :writerId")
  Page<AdoptionInfo> findByWriter_Id(@Param("writerId") UUID writerId, Pageable pageable);

  // clearAutomatically = true : 벌크 연산 후 영속성 컨텍스트 초기화 (권장)
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM AdoptionInfo a WHERE a.writer.id = :writerId AND a.completed = false")
  void deleteByWriterAndCompletedIsFalse(@Param("writerId") UUID writerId);

  // @Modifying(clearAutomatically = true)
  // @Query("DELETE FROM AdoptionImage a WHERE a.adoptionInfo.writer.id =
  // :writerId AND a.adoptionInfo.completed = false")
  // void deleteImagesByWriterAndCompletedIsFalse(@Param("writerId") UUID
  // writerId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM AdoptionImage a " +
      "WHERE a.adoptionInfo.id IN (" +
      "   SELECT i.id FROM AdoptionInfo i " +
      "   WHERE i.writer.id = :writerId AND i.completed = false" +
      ")")
  void deleteImagesByWriterAndCompletedIsFalse(@Param("writerId") UUID writerId);

  @Query("SELECT a.storeFileName FROM AdoptionImage a " +
    "WHERE a.adoptionInfo.id IN (" + 
      "   SELECT i.id FROM AdoptionInfo i " +
      "   WHERE i.writer.id = :writerId AND i.completed = false" +
      ")")
  List<String> findStoreFileNamesByWriterAndCompletedIsFalse(@Param("writerId") UUID writerId);
}
