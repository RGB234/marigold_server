package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class StorageFileReferenceRepositoryTest {

  @Test
  void returnsDistinctStoredFileNames() {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    given(jdbcTemplate.queryForList(anyString(), eq(String.class)))
        .willReturn(List.of("one.jpg", "one.jpg", "two.jpg"));

    StorageFileReferenceRepository repository = new StorageFileReferenceRepository(jdbcTemplate);

    assertThat(repository.findAllStoredFileNames()).containsExactlyInAnyOrder("one.jpg", "two.jpg");
  }
}
