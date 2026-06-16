package com.sns.marigold.global.validation.enumvalue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/*
  target으로 지정한 Enum Class의 constants 내에 있는 Enum 값인지 검증
*/
@Constraint(validatedBy = EnumValueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER}) // 어노테이션이 붙는 범위
@Retention(RetentionPolicy.RUNTIME) // 어노테이션 생명주기
public @interface EnumValue {
  String message() default "Invalid enum value"; // 예외 발생 응답

  Class<?>[] groups() default {}; // Validation 그룹 지정

  Class<? extends Payload>[] payload() default {}; // 추가 정보 제공

  Class<? extends java.lang.Enum<?>> target(); // Validation 적용 범위
}
