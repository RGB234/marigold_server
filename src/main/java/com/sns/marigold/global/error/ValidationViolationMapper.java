package com.sns.marigold.global.error;

import java.util.List;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import com.sns.marigold.global.error.dto.ValidationViolation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationViolationMapper {

  public static List<ValidationViolation> from(BindingResult bindingResult) {
    return bindingResult.getAllErrors().stream().map(ValidationViolationMapper::from).toList();
  }

  public static ValidationViolation from(ObjectError error) {
    String field =
        error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
    String message =
        error.getDefaultMessage() == null ? "입력값이 올바르지 않습니다." : error.getDefaultMessage();
    return ValidationViolation.body(field, message);
  }

  public static ValidationViolation from(MessageSourceResolvable error) {
    String message =
        error.getDefaultMessage() == null ? "입력값이 올바르지 않습니다." : error.getDefaultMessage();
    return ValidationViolation.parameter("request", message);
  }
}
