package com.sns.marigold.storage.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;

public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {

  List<StorageFile> findAllByStoredFileNameIn(Collection<String> storedFileNames);

  // 같은 트랜잭션에서 먼저 읽은 도메인 엔티티가 detach되지 않도록 영속성 컨텍스트를 비우지 않는다
  @Modifying
  @Query(
      "UPDATE StorageFile f SET f.status = :to, f.statusChangedAt = :now "
          + "WHERE f.storedFileName IN :names AND f.status = :from")
  int updateStatus(
      @Param("names") Collection<String> storedFileNames,
      @Param("from") StorageFileStatus from,
      @Param("to") StorageFileStatus to,
      @Param("now") LocalDateTime now);

  // dry-run에서도 같은 행을 다시 읽지 않도록 id 기준으로 넘긴다. 1000은 StorageFileInventory.PAGE_SIZE와 맞춘다
  List<StorageFile> findTop1000ByStatusAndCreatedAtBeforeAndIdGreaterThanOrderByIdAsc(
      StorageFileStatus status, LocalDateTime createdBefore, Long afterId);
}
