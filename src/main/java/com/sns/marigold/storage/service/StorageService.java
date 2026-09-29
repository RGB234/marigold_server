package com.sns.marigold.storage.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.ImageUploadDto;

public interface StorageService {

  ImageUploadDto uploadImage(MultipartFile file, StorageDirectory storageDirectory);

  List<ImageUploadDto> uploadImages(List<MultipartFile> images, StorageDirectory storageDirectory);

  List<FileUploadDto> uploadFiles(List<MultipartFile> files, StorageDirectory storageDirectory);

  /** 삭제 실패를 로그로 남기고 호출자에게 전파하지 않습니다. */
  void deleteFileBestEffort(String storedFileName);

  void deleteUploadedImagesBestEffort(List<ImageUploadDto> images);

  void deleteFilesBestEffort(List<String> storedFileNames);

  void deleteUploadedFilesBestEffort(List<FileUploadDto> files);

  String getViewUrlOrNull(String storedFileName);

  String getDownloadUrl(String storedFileName, String originalFileName);
}
