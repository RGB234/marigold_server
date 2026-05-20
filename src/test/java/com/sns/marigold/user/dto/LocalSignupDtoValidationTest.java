package com.sns.marigold.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sns.marigold.user.dto.create.LocalSignupDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LocalSignupDtoValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void nicknameAllowsTwoCharacters() {
    LocalSignupDto dto = signupDto("tester@example.com", "password", "가나");

    Set<ConstraintViolation<LocalSignupDto>> violations = validator.validate(dto);

    assertThat(violations).isEmpty();
  }

  @Test
  void nicknameRejectsOneCharacter() {
    LocalSignupDto dto = signupDto("tester@example.com", "password", "가");

    Set<ConstraintViolation<LocalSignupDto>> violations = validator.validate(dto);

    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("nickname"));
  }

  @Test
  void passwordRequiresEightCharacters() {
    LocalSignupDto dto = signupDto("tester@example.com", "pass", "가나");

    Set<ConstraintViolation<LocalSignupDto>> violations = validator.validate(dto);

    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
  }

  private LocalSignupDto signupDto(String email, String password, String nickname) {
    LocalSignupDto dto = new LocalSignupDto();
    ReflectionTestUtils.setField(dto, "email", email);
    ReflectionTestUtils.setField(dto, "password", password);
    ReflectionTestUtils.setField(dto, "nickname", nickname);
    return dto;
  }
}
