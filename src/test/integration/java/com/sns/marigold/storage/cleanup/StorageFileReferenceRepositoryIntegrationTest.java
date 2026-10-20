package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sns.marigold.support.BaseIntegrationTest;

class StorageFileReferenceRepositoryIntegrationTest extends BaseIntegrationTest {

  @Autowired private StorageFileReferenceRepository repository;

  @Test
  void findsReferencesAcrossAllStorageTables() {
    assertThat(repository.findAllStoredFileNames()).isEmpty();
  }
}
