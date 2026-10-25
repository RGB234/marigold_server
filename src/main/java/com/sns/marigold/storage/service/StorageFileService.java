package com.sns.marigold.storage.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.repository.StorageFileRepository;

import lombok.RequiredArgsConstructor;

/** 파일 행의 상태 전이. */
@Service
@RequiredArgsConstructor
public class StorageFileService {

  private final StorageFileRepository storageFileRepository;

  /**
   * 확정한 업로드를 ATTACHED로 바꾸고 도메인 행이 참조할 엔티티를 돌려준다. 이미 쓴 업로드가 섞여 있으면 실패해 트랜잭션 전체가 롤백된다.
   *
   * @return 입력과 같은 순서의 파일 엔티티
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public List<StorageFile> attach(List<FileUploadDto> confirmedFiles) {
    if (confirmedFiles.isEmpty()) {
      return List.of();
    }
    List<String> names = confirmedFiles.stream().map(FileUploadDto::getStoredFileName).toList();
    int attached =
        storageFileRepository.updateStatus(
            names, StorageFileStatus.PENDING, StorageFileStatus.ATTACHED, LocalDateTime.now());
    if (attached != names.size()) {
      throw StorageException.forUploadAlreadyUsed(names.size(), attached);
    }

    Map<String, StorageFile> filesByName =
        storageFileRepository.findAllByStoredFileNameIn(names).stream()
            .collect(Collectors.toMap(StorageFile::getStoredFileName, Function.identity()));
    return names.stream().map(filesByName::get).toList();
  }

  /** 만료되어 더 확정될 수 없는 PENDING을 DELETED로 바꾼다. 고아 정리가 파일을 지운 뒤 호출한다. */
  @Transactional
  public void expirePending(List<String> storedFileNames) {
    if (storedFileNames.isEmpty()) {
      return;
    }
    storageFileRepository.updateStatus(
        storedFileNames, StorageFileStatus.PENDING, StorageFileStatus.DELETED, LocalDateTime.now());
  }

  /** 교체·삭제로 더 참조하지 않는 파일을 DELETED로 바꾼다. 저장소 삭제는 커밋 후 이벤트 리스너가 한다. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void markDeleted(List<String> storedFileNames) {
    if (storedFileNames.isEmpty()) {
      return;
    }
    storageFileRepository.updateStatus(
        storedFileNames,
        StorageFileStatus.ATTACHED,
        StorageFileStatus.DELETED,
        LocalDateTime.now());
  }
}
