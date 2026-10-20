package com.sns.marigold.storage.cleanup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sns.marigold.storage.config.LocalStorageProperties;
import com.sns.marigold.storage.service.StorageDirectory;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
@RequiredArgsConstructor
public class LocalStorageFileInventory implements StorageFileInventory {

  private final LocalStorageProperties properties;

  @Override
  public List<StoredFile> listStoredFiles() {
    Path root = storageRoot();
    if (Files.notExists(root)) {
      return List.of();
    }

    try (Stream<Path> paths = Files.walk(root)) {
      return paths
          .filter(Files::isRegularFile)
          .map(path -> toStoredFile(root, path))
          .filter(Objects::nonNull)
          .toList();
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
