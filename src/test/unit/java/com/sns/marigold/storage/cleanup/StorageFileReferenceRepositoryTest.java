package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class StorageFileReferenceRepositoryTest {

  private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
  private final StorageFileReferenceRepository repository =
      new StorageFileReferenceRepository(jdbcTemplate);

  @Test
  void returnsDistinctReferencedFileNames() {
    given(jdbcTemplate.queryForList(anyString(), anyMap(), eq(String.class)))
        .willReturn(List.of("one.jpg", "one.jpg", "two.jpg"));

    assertThat(repository.findReferenced(List.of("one.jpg", "two.jpg", "three.jpg")))
        .containsExactlyInAnyOrder("one.jpg", "two.jpg");
  }

  @Test
  void emptyInputSkipsQuery() {
    assertThat(repository.findReferenced(List.of())).isEmpty();

    verifyNoInteractions(jdbcTemplate);
  }
}
