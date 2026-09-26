package com.sns.marigold.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.storage.dto.FileUploadDto;
import com.sns.marigold.storage.dto.ImageUploadDto;
import com.sns.marigold.storage.exception.StorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStorageService implements StorageService {

  @Override
  public ImageUploadDto uploadImage(MultipartFile file, StorageDirectory storageDirectory) {
    String detectedContentType = validateImageFile(file);
    return uploadValidatedImage(file, storageDirectory, detectedContentType);
  }

  @Override
  public List<ImageUploadDto> uploadImages(
      List<MultipartFile> images, StorageDirectory storageDirectory) {
    if (images == null || images.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> detectedContentTypes = images.stream().map(this::validateImageFile).toList();
    List<ImageUploadDto> result = new ArrayList<>();
    try {
      for (int index = 0; index < images.size(); index++) {
        result.add(
            uploadValidatedImage(
                images.get(index), storageDirectory, detectedContentTypes.get(index)));
      }
    } catch (StorageException e) {
      this.deleteUploadedImages(result);
      throw e;
    }
    return result;
  }

  @Override
  public List<FileUploadDto> uploadFiles(
      List<MultipartFile> files, StorageDirectory storageDirectory) {
    if (files == null || files.isEmpty()) {
      return Collections.emptyList();
    }
    List<FileUploadDto> result = new ArrayList<>();
    try {
      for (MultipartFile file : files) {
        result.add(this.uploadFileWithMetadata(file, storageDirectory, null));
      }
    } catch (StorageException e) {
      this.deleteUploadedFiles(result);
      throw e;
    }
    return result;
  }

  @Override
  public void deleteFile(String storedFileName) {
    Objects.requireNonNull(storedFileName, "storedFileName must not be null");
    try {
      validateStoredFileName(storedFileName);
      deleteStoredFile(storedFileName);
    } catch (Exception e) {
      log.warn("event=storage_delete_failed storedFileName={}", storedFileName, e);
    }
  }

  @Override
  public void deleteUploadedImages(List<ImageUploadDto> images) {
    for (ImageUploadDto dto : images) {
      this.deleteFile(dto.getStoredFileName());
    }
  }

  @Override
  public void deleteUploadedImagesByStoredFileNames(List<String> storedFileNames) {
    for (String storedFileName : storedFileNames) {
      this.deleteFile(storedFileName);
    }
  }

  @Override
  public void deleteUploadedFiles(List<FileUploadDto> files) {
    for (FileUploadDto dto : files) {
      this.deleteFile(dto.getStoredFileName());
    }
  }

  protected abstract FileUploadDto uploadFileWithMetadata(
      MultipartFile file, StorageDirectory storageDirectory, String contentTypeOverride);

  protected abstract void deleteStoredFile(String storedFileName) throws IOException;

  protected StorageFileInfo prepareFile(
      MultipartFile file, StorageDirectory storageDirectory, String contentTypeOverride) {
    if (file == null || file.isEmpty()) {
      throw StorageException.forEmptyFile();
    }

    Objects.requireNonNull(storageDirectory, "storageDirectory must not be null");

    String originalFilename = file.getOriginalFilename();
    String storedFileName = generateStoredFileName(originalFilename, storageDirectory);
    String contentType =
        contentTypeOverride != null
            ? contentTypeOverride
            : file.getContentType() != null ? file.getContentType() : "application/octet-stream";

    return new StorageFileInfo(storedFileName, originalFilename, contentType, file.getSize());
  }

  private ImageUploadDto uploadValidatedImage(
      MultipartFile file, StorageDirectory storageDirectory, String detectedContentType) {
    FileUploadDto uploadedFile =
        uploadFileWithMetadata(file, storageDirectory, detectedContentType);
    return ImageUploadDto.builder()
        .storedFileName(uploadedFile.getStoredFileName())
        .originalFileName(uploadedFile.getOriginalFileName())
        .build();
  }

  private String validateImageFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw StorageException.forEmptyFile();
    }
    if (file.getSize() > ValidationPolicy.Image.MAX_SIZE_BYTES) {
      throw StorageException.forFileSizeExceeded(
          file.getOriginalFilename(), file.getSize(), ValidationPolicy.Image.MAX_SIZE_BYTES);
    }

    String extension = normalizedFileExtension(file.getOriginalFilename());
    List<String> allowedMimeTypes =
        ValidationPolicy.Image.ALLOWED_MIME_TYPES_BY_EXTENSION.get(extension);
    if (allowedMimeTypes == null) {
      throw StorageException.forUnsupportedFileExtension(file.getOriginalFilename(), extension);
    }

    try (InputStream inputStream = file.getInputStream()) {
      String detectedContentType = new Tika().detect(inputStream);
      if (!allowedMimeTypes.contains(detectedContentType)) {
        throw StorageException.forInvalidMimeType(
            file.getOriginalFilename(), extension, detectedContentType);
      }
      return detectedContentType;
    } catch (IOException e) {
      throw StorageException.forFileReadFailed(file.getOriginalFilename(), e);
    }
  }

  private String normalizedFileExtension(String fileName) {
    return getFileExtension(fileName).substring(1).toLowerCase(Locale.ROOT);
  }

  protected String sanitizeContentDispositionFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "download";
    }
    return fileName.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
  }

  protected void validateStoredFileName(String storedFileName) {
    if (!StorageDirectory.isValidStoredFileName(storedFileName)) {
      throw StorageException.forFileNotFound();
    }
  }

  private String generateStoredFileName(String fileName, StorageDirectory storageDirectory) {
    return storageDirectory.path() + "/" + UUID.randomUUID() + getFileExtension(fileName);
  }

  private String getFileExtension(String fileName) {
    int extensionStart = fileName == null ? -1 : fileName.lastIndexOf(".");
    if (extensionStart < 0 || extensionStart == fileName.length() - 1) {
      throw StorageException.forMissingFileExtension(fileName);
    }
    return fileName.substring(extensionStart);
  }

  protected record StorageFileInfo(
      String storedFileName, String originalFileName, String contentType, long fileSize) {}
}
