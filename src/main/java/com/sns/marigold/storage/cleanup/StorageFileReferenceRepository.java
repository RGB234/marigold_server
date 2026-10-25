package com.sns.marigold.storage.cleanup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StorageFileReferenceRepository {

  // 도메인 테이블은 storage_file을 FK로 참조하므로 이 테이블만 보면 된다. DELETED는 지워야 할 파일이다
  private static final String FIND_REFERENCED_STORED_FILE_NAMES =
      """
      SELECT stored_file_name FROM storage_file
      WHERE stored_file_name IN (:names) AND status <> 'DELETED'
      """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /** 주어진 파일명 중 PENDING·ATTACHED 행이 있는 것만 돌려준다. */
  public Set<String> findReferenced(Collection<String> storedFileNames) {
    if (storedFileNames.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        jdbcTemplate.queryForList(
            FIND_REFERENCED_STORED_FILE_NAMES, Map.of("names", storedFileNames), String.class));
  }
}
