package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sns.marigold.storage.config.LocalStorageProperties;

class LocalStorageFileInventoryTest {

  @TempDir Path tempDir;

  @Test
  void listsOnlyManagedFiles() throws Exception {
    String storedFileName = "adoption/post/11111111-1111-1111-1111-111111111111.jpg";
    Instant lastModified = Instant.parse("2026-10-01T00:00:00Z");
    Path storedFile = tempDir.resolve(storedFileName);
    Files.createDirectories(storedFile.getParent());
    Files.writeString(storedFile, "image");
    Files.setLastModifiedTime(storedFile, FileTime.from(lastModified));
    Files.writeString(tempDir.resolve("unmanaged.txt"), "keep");

    LocalStorageFileInventory inventory =
        new LocalStorageFileInventory(
            new LocalStorageProperties(tempDir.toString(), null, null, 0, 0));

    List<StoredFile> result = inventory.listStoredFiles();

    assertThat(result).containsExactly(new StoredFile(storedFileName, lastModified));
  }

  @Test
  void missingStorageRootReturnsEmptyList() {
    LocalStorageFileInventory inventory =
        new LocalStorageFileInventory(
            new LocalStorageProperties(tempDir.resolve("missing").toString(), null, null, 0, 0));

    assertThat(inventory.listStoredFiles()).isEmpty();
  }
}
