package com.sns.marigold.storage.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.ImageUploadDto;

public interface StorageService {

  ImageUploadDto uploadImage(MultipartFile file, StorageDirectory storageDirectory);

  List<ImageUploadDto> uploadImages(List<MultipartFile> images, StorageDirectory storageDirectory);

  List<FileUploadDto> uploadFiles(List<MultipartFile> files, StorageDirectory storageDirectory);

  void deleteFile(String storedFileName);

  void deleteUploadedImages(List<ImageUploadDto> images);

  void deleteUploadedImagesByStoredFileNames(List<String> storedFileNames);

  void deleteUploadedFiles(List<FileUploadDto> files);

  String getViewUrlOrNull(String storedFileName);

  String getDownloadUrl(String storedFileName, String originalFileName);
}
