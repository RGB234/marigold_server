package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionPostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionPostImageRepository
    extends JpaRepository<AdoptionPostImage, Long>, JpaSpecificationExecutor<AdoptionPostImage> {

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
