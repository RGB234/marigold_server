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

  // 파일을 저장하는 테이블을 추가하면 여기에도 넣어야 한다. 빠지면 고아 정리가 그 테이블의 파일을 지운다
  private static final String FIND_REFERENCED_STORED_FILE_NAMES =
      """
      SELECT stored_file_name FROM user_image WHERE stored_file_name IN (:names)
      UNION ALL
      SELECT stored_file_name FROM adoption_post_image WHERE stored_file_name IN (:names)
      UNION ALL
      SELECT stored_file_name FROM adoption_comment_image WHERE stored_file_name IN (:names)
      UNION ALL
      SELECT stored_file_name FROM chat_message_attachments WHERE stored_file_name IN (:names)
      """;

  private final NamedParameterJdbcTemplate jdbcTemplate;

  /** 주어진 파일명 중 DB가 참조하는 것만 돌려준다. */
  public Set<String> findReferenced(Collection<String> storedFileNames) {
    if (storedFileNames.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(
        jdbcTemplate.queryForList(
            FIND_REFERENCED_STORED_FILE_NAMES, Map.of("names", storedFileNames), String.class));
  }
}
