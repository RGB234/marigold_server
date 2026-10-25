package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sns.marigold.storage.StorageFileFixtures;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.repository.StorageFileRepository;

@ExtendWith(MockitoExtension.class)
class StorageFileServiceTest {

  private static final String FIRST = "adoption/post/11111111-1111-1111-1111-111111111111.jpg";
  private static final String SECOND = "adoption/post/22222222-2222-2222-2222-222222222222.jpg";

  @Mock private StorageFileRepository storageFileRepository;

  private StorageFileService storageFileService;

  @BeforeEach
  void setUp() {
    storageFileService = new StorageFileService(storageFileRepository);
  }

  @Test
  void attach_ReturnsFilesInRequestOrder() {
    given(
            storageFileRepository.updateStatus(
                eq(List.of(FIRST, SECOND)),
                eq(StorageFileStatus.PENDING),
                eq(StorageFileStatus.ATTACHED),
                any(LocalDateTime.class)))
        .willReturn(2);
    StorageFile first = StorageFileFixtures.attached(FIRST, "first.jpg");
    StorageFile second = StorageFileFixtures.attached(SECOND, "second.jpg");
    given(storageFileRepository.findAllByStoredFileNameIn(List.of(FIRST, SECOND)))
        .willReturn(List.of(second, first));

    List<StorageFile> attached =
        storageFileService.attach(List.of(confirmed(FIRST), confirmed(SECOND)));

    assertThat(attached).containsExactly(first, second);
  }

  @Test
  void attach_FailsWhenAnyUploadIsNotPending() {
    given(
            storageFileRepository.updateStatus(
                eq(List.of(FIRST, SECOND)),
                eq(StorageFileStatus.PENDING),
                eq(StorageFileStatus.ATTACHED),
                any(LocalDateTime.class)))
        .willReturn(1);

    assertThatThrownBy(
            () -> storageFileService.attach(List.of(confirmed(FIRST), confirmed(SECOND))))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorSpec())
        .isEqualTo(StorageError.UPLOAD_ALREADY_USED);
    verify(storageFileRepository, never()).findAllByStoredFileNameIn(any());
  }

  @Test
  void markDeleted_OnlyChangesAttachedFiles() {
    storageFileService.markDeleted(List.of(FIRST));

    verify(storageFileRepository)
        .updateStatus(
            eq(List.of(FIRST)),
            eq(StorageFileStatus.ATTACHED),
            eq(StorageFileStatus.DELETED),
            any(LocalDateTime.class));
  }

  @Test
  void expirePending_OnlyChangesPendingFiles() {
    storageFileService.expirePending(List.of(FIRST));

    verify(storageFileRepository)
        .updateStatus(
            eq(List.of(FIRST)),
            eq(StorageFileStatus.PENDING),
            eq(StorageFileStatus.DELETED),
            any(LocalDateTime.class));
  }

  private static FileUploadDto confirmed(String storedFileName) {
    return FileUploadDto.builder()
        .storedFileName(storedFileName)
        .originalFileName("original.jpg")
        .contentType("image/jpeg")
        .fileSize(1L)
        .build();
  }
}
