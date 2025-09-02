package com.sns.marigold.global.annotation;

import com.sns.marigold.global.validator.EnumValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER}) // 어노테이션이 붙는 범위
@Retention(RetentionPolicy.RUNTIME) // 어노테이션 생명주기
public @interface Enum {
  String message() default "Invalid enum value"; // 예외 발생 응답

  Class<?>[] groups() default {}; // Validation 그룹 지정

  Class<? extends Payload>[] payload() default {}; // 추가 정보 제공

  Class<? extends java.lang.Enum<?>> target(); // Validation 적용 범위
}
