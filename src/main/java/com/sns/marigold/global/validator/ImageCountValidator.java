package com.sns.marigold.global.validator;

import com.sns.marigold.global.annotation.ValidImageCount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;

public class ImageCountValidator
    implements ConstraintValidator<ValidImageCount, ImageCountValidatable> {
  private int min;
  private int max;

  @Override
  public void initialize(ValidImageCount annotation) {
    this.min = annotation.min();
    this.max = annotation.max();
  }

  @Override
  public boolean isValid(
      ImageCountValidatable imageCountValidatable, ConstraintValidatorContext context) {
    if (imageCountValidatable == null) {
      return true;
    }

    List<String> imagesToKeep = imageCountValidatable.getImagesToKeep();
    int storedImageCount =
        imagesToKeep == null || imagesToKeep.isEmpty()
            ? 0
            : (int)
                imagesToKeep.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(fileName -> !fileName.isEmpty())
                    .distinct()
                    .count();

    List<MultipartFile> images = imageCountValidatable.getImages();
    int newImageCount =
        images == null || images.isEmpty()
            ? 0
            : (int)
                images.stream()
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
          // 설정안할 시 해당 Validator가 호출된 필드명 혹은 클래스(여기서는 DTO) 인스턴스 이름으로 자동 매핑
          .addPropertyNode("images")
          .addConstraintViolation();
      return false;
    }
    return true;
  }
}
