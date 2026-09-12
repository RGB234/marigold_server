package com.sns.marigold.global.validation.imagefile;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.global.validation.ValidationPolicy;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.StorageService;

import jakarta.validation.ConstraintValidatorContext;

final class ImageFileValidatorSupport {
  private ImageFileValidatorSupport() {}

  static boolean isValid(
      MultipartFile file, StorageService storageService, ConstraintValidatorContext context) {
    if (file == null || file.isEmpty()) {
      return true;
    }
    return isValid(List.of(file), storageService, context);
  }

  static boolean isValid(
      List<MultipartFile> files, StorageService storageService, ConstraintValidatorContext context) {
    if (files == null || files.isEmpty()) {
      return true;
    }

    List<MultipartFile> nonEmptyFiles =
        files.stream().filter(file -> file != null && !file.isEmpty()).toList();

    if (nonEmptyFiles.isEmpty()) {
      return true;
    }

    if (nonEmptyFiles.stream()
        .anyMatch(file -> file.getSize() > ValidationPolicy.Image.MAX_SIZE_BYTES)) {
      replaceMessage(context, "파일 크기는 최대 " + ValidationPolicy.Image.MAX_SIZE_MB + "MB까지 가능합니다.");
      return false;
    }

    try {
      storageService.validateRealImageFiles(nonEmptyFiles);
    } catch (StorageException e) {
      replaceMessage(context, "JPG, JPEG, PNG, WebP 형식의 이미지만 업로드 가능합니다.");
      return false;
    }
    return true;
  }

  private static void replaceMessage(ConstraintValidatorContext context, String message) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}
