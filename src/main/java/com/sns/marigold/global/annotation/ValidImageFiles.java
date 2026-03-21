package com.sns.marigold.global.annotation;

import com.sns.marigold.global.validator.ImageFilesValidatorForList;
import com.sns.marigold.global.validator.ImageFilesValidatorForSingle;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = {ImageFilesValidatorForList.class, ImageFilesValidatorForSingle.class})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageFiles {
  String message() default "유효하지 않은 이미지 파일입니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
