package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    List<List<StoredFile>> pages = new ArrayList<>();
    inventory(tempDir).forEachPage(pages::add);

    assertThat(pages).containsExactly(List.of(new StoredFile(storedFileName, lastModified)));
  }

  @Test
  void splitsFilesIntoPagesOfAtMostPageSize() throws Exception {
    Path directory = tempDir.resolve("adoption/post");
    Files.createDirectories(directory);
    for (int i = 0; i <= StorageFileInventory.PAGE_SIZE; i++) {
      Files.writeString(directory.resolve(UUID.randomUUID() + ".jpg"), "image");
    }

    List<List<StoredFile>> pages = new ArrayList<>();
    inventory(tempDir).forEachPage(pages::add);

    assertThat(pages).extracting(List::size).containsExactly(StorageFileInventory.PAGE_SIZE, 1);
  }

  @Test
  void missingStorageRootPassesNoPage() {
    List<List<StoredFile>> pages = new ArrayList<>();
    inventory(tempDir.resolve("missing")).forEachPage(pages::add);

    assertThat(pages).isEmpty();
  }

  private LocalStorageFileInventory inventory(Path root) {
    return new LocalStorageFileInventory(
        new LocalStorageProperties(root.toString(), null, null, 0, 0));
  }
}
