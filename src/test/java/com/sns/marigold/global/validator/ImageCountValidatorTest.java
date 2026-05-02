package com.sns.marigold.global.validator;

import static org.assertj.core.api.Assertions.assertThat;

import com.sns.marigold.adoption.dto.AdoptionPostUpdateDto;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageCountValidatorTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  @DisplayName("유지할 이미지 파일명이 공백이면 기존 이미지 개수로 세지 않는다.")
  void blankImagesToKeep_DoesNotCountAsStoredImage() {
    AdoptionPostUpdateDto dto =
        AdoptionPostUpdateDto.builder()
            .title("Updated Title")
            .species(Species.DOG)
            .sex(Sex.MALE)
            .age(3)
            .weight(6.0)
            .area("Seoul")
            .neutering(Neutering.YES)
            .features("Updated features text")
            .imagesToKeep(List.of(" "))
            .images(List.of())
            .build();

    Set<ConstraintViolation<AdoptionPostUpdateDto>> violations = validator.validate(dto);

    assertThat(violations)
        .anySatisfy(
            violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("images"));
  }
}
