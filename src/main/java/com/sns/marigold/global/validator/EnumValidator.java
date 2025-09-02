package com.sns.marigold.global.validator;

import com.sns.marigold.global.annotation.Enum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Defines the logic to validate a given constraint A for a given object type T.
public class EnumValidator implements ConstraintValidator<Enum, java.lang.Enum> {

  private Enum annotation;

  @Override
  public void initialize(Enum constraintAnnotation) {
    this.annotation = constraintAnnotation;
  }

  @Override
  public boolean isValid(java.lang.Enum value, ConstraintValidatorContext context) {
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

  // when input data is ensured to be valid
  // E.X. GenderType enum class ensures validation by JSON deserialization method
  //    @Override
  //    public boolean isValid(java.lang.Enum value, ConstraintValidatorContext context){
  //        return value != null;
  //    }
}
