package com.sns.marigold.adoption.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sns.marigold.adoption.entity.AdoptionComment;

public interface AdoptionCommentRepository extends JpaRepository<AdoptionComment, Long> {

  @Query(
      "SELECT c FROM AdoptionComment c LEFT JOIN FETCH c.writer w LEFT JOIN FETCH w.image wi "
          + "LEFT JOIN FETCH c.images ci "
          + "WHERE c.adoptionPost.id = :postId "
          + "ORDER BY c.createdAt ASC")
  List<AdoptionComment> findByAdoptionPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);

  @Modifying
  @Query("UPDATE AdoptionComment c SET c.parent = null WHERE c.adoptionPost.id = :postId")
  void clearParentByAdoptionPostId(@Param("postId") Long postId);

  @Modifying
  @Query("DELETE FROM AdoptionComment c WHERE c.adoptionPost.id = :postId")
  void deleteByAdoptionPostId(@Param("postId") Long postId);
}
