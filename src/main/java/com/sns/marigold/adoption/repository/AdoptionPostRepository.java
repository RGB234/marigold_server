package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionPost;
import java.util.List;
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

  // clearAutomatically = true : 벌크 연산 후 영속성 컨텍스트 초기화 (권장)
  @Modifying(clearAutomatically = true)
  @Query("UPDATE AdoptionPost a SET a.deletedAt = CURRENT_TIMESTAMP WHERE a.writer.id = :writerId")
  void softDeleteByWriter(@Param("writerId") Long writerId);

  @Modifying(clearAutomatically = true)
  @Query(
      "DELETE FROM AdoptionPostImage a "
          + "WHERE a.adoptionPost.id IN ("
          + "   SELECT i.id FROM AdoptionPost i "
          + "   WHERE i.writer.id = :writerId"
          + ")")
  void deleteImagesByWriter(@Param("writerId") Long writerId);

  @Query(
      "SELECT a.storedFileName FROM AdoptionPostImage a "
          + "WHERE a.adoptionPost.id IN ("
          + "   SELECT i.id FROM AdoptionPost i "
          + "   WHERE i.writer.id = :writerId"
          + ")")
  List<String> findStoredFileNamesByWriter(@Param("writerId") Long writerId);
}
