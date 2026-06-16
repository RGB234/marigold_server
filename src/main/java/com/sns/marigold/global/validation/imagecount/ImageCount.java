package com.sns.marigold.global.validation.imagecount;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = ImageCountValidator.class)
@Target({ElementType.TYPE}) // 클래스
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageCount {
  String message() default "이미지 파일을 {min}개 이상 {max}개 이하로 업로드해주세요.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int min();

  int max();
}
