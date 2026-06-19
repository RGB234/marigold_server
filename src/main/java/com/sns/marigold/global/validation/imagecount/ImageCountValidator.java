package com.sns.marigold.global.validation.imagecount;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ImageCountValidator
    implements ConstraintValidator<ImageCount, ImageCountValidatable> {
  private int min;
  private int max;

  @Override
  public void initialize(ImageCount annotation) {
    this.min = annotation.min();
    this.max = annotation.max();
  }

  @Override
  public boolean isValid(
      ImageCountValidatable imageCountValidatable, ConstraintValidatorContext context) {
    if (imageCountValidatable == null) {
      return true;
    }

    // 기존 이미지
    List<String> imagesToKeep = imageCountValidatable.getImagesToKeep();
    int storedImageCount =
        imagesToKeep == null || imagesToKeep.isEmpty()
            ? 0
            : (int)
                imagesToKeep.stream()
                    .distinct()
                    .filter(f -> f != null && !f.isEmpty())
                    .count();

    // 새 업로드 파일
    List<MultipartFile> images = imageCountValidatable.getImages();
    int newImageCount =
        images == null || images.isEmpty()
            ? 0
            : (int) images.stream()
                    .filter(f -> f != null && !f.isEmpty())
                    .count();

    int totalImageCount = storedImageCount + newImageCount;

    if (totalImageCount < min || totalImageCount > max) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate(
              "이미지 파일을 {min}개 이상 {max}개 이하로 업로드해주세요."
                  .replace("{min}", String.valueOf(min))
                  .replace("{max}", String.valueOf(max)))
          // 에러를 발생시킬 필드명.
          // 설정안할 시 해당 Validator가 호출된 필드명 혹은 클래스 이름으로 자동 매핑
          .addPropertyNode("images")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
