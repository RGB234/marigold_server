package com.sns.marigold.storage;

import java.time.LocalDateTime;
import java.util.List;

import org.mockito.stubbing.Answer;

import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;

public final class StorageFileFixtures {

  private StorageFileFixtures() {}

  public static StorageFile attached(String storedFileName, String originalFileName) {
    return StorageFile.builder()
        .storedFileName(storedFileName)
        .originalFileName(originalFileName)
        .contentType("image/jpeg")
        .fileSize(1L)
        .status(StorageFileStatus.ATTACHED)
        .statusChangedAt(LocalDateTime.now())
        .build();
  }

  /** {@code StorageFileService.attach} mock이 확정 결과를 그대로 ATTACHED 엔티티로 돌려주게 한다. */
  public static Answer<List<StorageFile>> attachConfirmed() {
    return invocation -> {
      List<FileUploadDto> files = invocation.getArgument(0);
      return files.stream()
          .map(
              file ->
                  StorageFile.builder()
                      .storedFileName(file.getStoredFileName())
                      .originalFileName(file.getOriginalFileName())
                      .contentType(file.getContentType())
                      .fileSize(file.getFileSize())
                      .status(StorageFileStatus.ATTACHED)
                      .statusChangedAt(LocalDateTime.now())
                      .build())
          .toList();
    };
  }
}
