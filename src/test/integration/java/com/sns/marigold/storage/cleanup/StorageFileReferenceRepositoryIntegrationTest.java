package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sns.marigold.support.BaseIntegrationTest;

class StorageFileReferenceRepositoryIntegrationTest extends BaseIntegrationTest {

  @Autowired private StorageFileReferenceRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void findsPendingAndAttachedFilesOnly() {
    insertStorageFile("adoption/post/pending.jpg", "PENDING");
    insertStorageFile("adoption/post/attached.jpg", "ATTACHED");
    insertStorageFile("adoption/post/deleted.jpg", "DELETED");

    assertThat(
            repository.findReferenced(
                List.of(
                    "adoption/post/pending.jpg",
                    "adoption/post/attached.jpg",
                    "adoption/post/deleted.jpg",
                    "adoption/post/unknown.jpg")))
        .containsExactlyInAnyOrder("adoption/post/pending.jpg", "adoption/post/attached.jpg");
  }

  private void insertStorageFile(String storedFileName, String status) {
    jdbcTemplate.update(
        "INSERT INTO storage_file (stored_file_name, original_file_name, content_type, file_size,"
            + " status, created_at, status_changed_at) VALUES (?, 'a.jpg', 'image/jpeg', 1, ?,"
            + " NOW(), NOW())",
        storedFileName,
        status);
  }
}
