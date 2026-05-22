package com.sns.marigold.adoption.repository;

import com.sns.marigold.adoption.entity.AdoptionCommentImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionCommentImageRepository extends JpaRepository<AdoptionCommentImage, Long> {

  @Query(
      "SELECT i.storedFileName FROM AdoptionCommentImage i "
          + "WHERE i.adoptionComment.id IN ("
          + "  SELECT c.id FROM AdoptionComment c WHERE c.adoptionPost.id = :postId"
          + ")")
  List<String> findStoredFileNamesByAdoptionPostId(@Param("postId") Long postId);

  @Modifying
  @Query(
      "DELETE FROM AdoptionCommentImage i "
          + "WHERE i.adoptionComment.id IN ("
          + "  SELECT c.id FROM AdoptionComment c WHERE c.adoptionPost.id = :postId"
          + ")")
  void deleteByAdoptionPostId(@Param("postId") Long postId);
}
