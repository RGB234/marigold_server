package com.sns.marigold.global.validation.imagefile;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/*
 1. 이미지 파일 크기 제한 (ValidationPolicy.Image.MAX_SIZE_MB).
 2. 이미지 파일이 맞는지 검증
*/
@Constraint(validatedBy = {ImageFileValidatorForList.class, ImageFileValidatorForSingle.class})
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageFile {
  String message() default "유효하지 않은 이미지 파일입니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
