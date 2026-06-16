package com.sns.marigold.global.validation.enumvalue;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Defines the logic to validate a given constraint A for a given object type T.
public class EnumValueValidator implements ConstraintValidator<EnumValue, java.lang.Enum<?>> {

  private EnumValue annotation;

  @Override
  public void initialize(EnumValue constraintAnnotation) {
    this.annotation = constraintAnnotation;
  }

  @Override
  public boolean isValid(java.lang.Enum<?> value, ConstraintValidatorContext context) {
    Object[] enumConstants = this.annotation.target().getEnumConstants();
    if (enumConstants != null
        // null if this Enum annotation does not represent java.lang.Enum object
        && value != null // null if JsonParser failed to parse JSON to an ENUM object
    ) {
      for (Object enumConstant : enumConstants) {
        if (enumConstant.toString().equals(value.toString())) {
          return true;
        }
      }
    }
    return false;
  }
}
