package com.sns.marigold.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.sns.marigold.global.validator.ImageFilesValidator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = ImageFilesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImageFiles {
    String message() default "이미지 파일을 최소 1개 이상 업로드해주세요.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}