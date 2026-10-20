package com.sns.marigold.storage.cleanup;

import java.util.HashSet;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class StorageFileReferenceRepository {

  private static final String FIND_ALL_STORED_FILE_NAMES =
      """
      SELECT stored_file_name FROM user_image
      UNION ALL
      SELECT stored_file_name FROM adoption_post_image
      UNION ALL
      SELECT stored_file_name FROM adoption_comment_image
      UNION ALL
      SELECT stored_file_name FROM chat_message_attachments
      """;

  private final JdbcTemplate jdbcTemplate;

  public Set<String> findAllStoredFileNames() {
    return new HashSet<>(jdbcTemplate.queryForList(FIND_ALL_STORED_FILE_NAMES, String.class));
  }
}
