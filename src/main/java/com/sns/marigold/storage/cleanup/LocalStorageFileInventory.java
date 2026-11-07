package com.sns.marigold.storage.cleanup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.objectstore.StorageDirectory;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
@RequiredArgsConstructor
public class LocalStorageFileInventory implements StorageFileInventory {

  private final LocalStorageProperties properties;

  @Override
  public void forEachPage(Consumer<List<StoredFile>> pageConsumer) {
    Path root = storageRoot();
    if (Files.notExists(root)) {
      return;
    }

    try (Stream<Path> paths = Files.walk(root)) {
      Iterator<StoredFile> files =
          paths
              .filter(Files::isRegularFile)
              .map(path -> toStoredFile(root, path))
              .filter(Objects::nonNull)
              .iterator();
      List<StoredFile> page = new ArrayList<>(PAGE_SIZE);
      while (files.hasNext()) {
        page.add(files.next());
        if (page.size() == PAGE_SIZE) {
          pageConsumer.accept(List.copyOf(page));
          page.clear();
        }
      }
      if (!page.isEmpty()) {
        pageConsumer.accept(List.copyOf(page));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to list local storage files", e);
    }
  }

  private StoredFile toStoredFile(Path root, Path path) {
    String storedFileName = root.relativize(path).toString().replace(File.separatorChar, '/');
    if (!StorageDirectory.isValidStoredFileName(storedFileName)) {
      return null;
    }

    try {
      return new StoredFile(storedFileName, Files.getLastModifiedTime(path).toInstant());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read local storage file metadata", e);
    }
  }

  private Path storageRoot() {
    return Path.of(
            Objects.requireNonNull(
                properties.rootPath(), "local storage root path must not be null"))
        .toAbsolutePath()
        .normalize();
  }
}
