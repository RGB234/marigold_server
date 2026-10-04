package com.sns.marigold.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sns.marigold.support.BaseIntegrationTest;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.exception.UserError;
import com.sns.marigold.user.repository.UserRepository;

class UniqueConstraintMigrationTest extends BaseIntegrationTest {
  @Autowired private JdbcTemplate jdbc;
  @Autowired private UserRepository users;

  @Test
  void migratesLegacyNamesAndPreservesUniqueDomainMapping() throws Exception {
    jdbc.execute("ALTER TABLE users RENAME INDEX uk_users_nickname TO legacy_nickname");
    jdbc.execute("ALTER TABLE users ADD UNIQUE INDEX duplicate_nickname (nickname)");
    String migration =
        Files.readString(
            Path.of("docs/migrations/2026-10-04-error-policy-unique-constraints.sql"),
            StandardCharsets.UTF_8);
    String procedure =
        migration.substring(migration.indexOf("CREATE PROCEDURE"), migration.indexOf("END$$") + 3);
    jdbc.execute(procedure);
    String calls = migration.substring(migration.indexOf("DELIMITER ;") + "DELIMITER ;".length());
    for (String statement : calls.split(";")) {
      if (!statement.isBlank()) {
        jdbc.execute(statement);
      }
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'nickname' AND non_unique = 0",
                Integer.class))
        .isEqualTo(1);
    users.saveAndFlush(User.builder().nickname("duplicate").build());
    Throwable duplicate =
        catchThrowable(() -> users.saveAndFlush(User.builder().nickname("duplicate").build()));
    assertThat(UniqueConflictPolicy.resolve(duplicate))
        .isEqualTo(UserError.NICKNAME_ALREADY_EXISTS);
  }
}
