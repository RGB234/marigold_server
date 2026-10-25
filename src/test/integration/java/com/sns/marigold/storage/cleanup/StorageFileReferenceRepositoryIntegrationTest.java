package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sns.marigold.support.BaseIntegrationTest;

class StorageFileReferenceRepositoryIntegrationTest extends BaseIntegrationTest {

  @Autowired private StorageFileReferenceRepository repository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void findsReferencesAcrossAllStorageTables() {
    insertReferenceRows();

    assertThat(
            repository.findReferenced(
                List.of(
                    "user/profile/a.jpg",
                    "adoption/post/b.jpg",
                    "adoption/comment/c.jpg",
                    "chat/attachment/d.txt",
                    "adoption/post/unreferenced.jpg")))
        .containsExactlyInAnyOrder(
            "user/profile/a.jpg",
            "adoption/post/b.jpg",
            "adoption/comment/c.jpg",
            "chat/attachment/d.txt");
  }

  // 부모 행 없이 파일 참조 행만 만들려고 같은 커넥션에서 FK 검사를 끈다
  private void insertReferenceRows() {
    jdbcTemplate.execute(
        (ConnectionCallback<Void>)
            connection -> {
              try (Statement statement = connection.createStatement()) {
                statement.execute("SET FOREIGN_KEY_CHECKS = 0");
                statement.executeUpdate(
                    "INSERT INTO user_image (created_at, stored_file_name, original_file_name,"
                        + " user_id) VALUES (NOW(), 'user/profile/a.jpg', 'a.jpg', 1)");
                statement.executeUpdate(
                    "INSERT INTO adoption_post_image (created_at, stored_file_name,"
                        + " original_file_name, adoption_post_id)"
                        + " VALUES (NOW(), 'adoption/post/b.jpg', 'b.jpg', 1)");
                statement.executeUpdate(
                    "INSERT INTO adoption_comment_image (created_at, stored_file_name,"
                        + " original_file_name, adoption_comment_id)"
                        + " VALUES (NOW(), 'adoption/comment/c.jpg', 'c.jpg', 1)");
                statement.executeUpdate(
                    "INSERT INTO chat_message_attachments (created_at, stored_file_name,"
                        + " original_file_name, content_type, file_size, chat_message_id)"
                        + " VALUES (NOW(), 'chat/attachment/d.txt', 'd.txt', 'text/plain', 1, 1)");
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
              }
              return null;
            });
  }
}
