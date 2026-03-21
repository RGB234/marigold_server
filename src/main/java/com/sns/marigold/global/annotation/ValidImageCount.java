package com.sns.marigold.global.annotation;

import com.sns.marigold.global.validator.ImageCountValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ImageCountValidator.class)
@Target({ElementType.TYPE}) // 클래스
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageCount {
  String message() default "이미지 파일을 {min}개 이상 {max}개 이하로 업로드해주세요.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int min() default 1;

  int max() default 8;
}
