package com.sns.marigold.global.validator;

import com.sns.marigold.global.annotation.ValidImageFiles;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.S3Service;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

public class ImageFilesValidatorForSingle
    implements ConstraintValidator<ValidImageFiles, MultipartFile> {
  @Autowired private S3Service s3Service;

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    if (file == null || file.isEmpty()) return true;

    List<MultipartFile> nonEmptyFiles = List.of(file);

    // List 형태로 변환하여 기존 S3 검증 로직 재사용
    try {
      s3Service.validateRealImageFiles(nonEmptyFiles);
    } catch (StorageException e) {
      replaceMessage(context, "JPG, JPEG, PNG, WebP 형식의 이미지만 업로드 가능합니다.");
      return false;
    }
    return true;
  }

  private void replaceMessage(ConstraintValidatorContext ctx, String message) {
    ctx.disableDefaultConstraintViolation();
    ctx.buildConstraintViolationWithTemplate(message).addConstraintViolation();
  }
}
