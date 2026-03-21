package com.sns.marigold.global.validator;

import com.sns.marigold.global.annotation.ValidImageCount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
    int storedImageCount = imageCountValidatable.getImagesToKeep().size();

    int newImageCount =
        imageCountValidatable.getImages() == null || imageCountValidatable.getImages().isEmpty()
            ? 0
            : (int)
                imageCountValidatable.getImages().stream()
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
