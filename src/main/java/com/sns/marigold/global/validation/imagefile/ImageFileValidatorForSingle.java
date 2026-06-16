package com.sns.marigold.global.validation.imagefile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.service.S3Service;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ImageFileValidatorForSingle
    implements ConstraintValidator<ImageFile, MultipartFile> {
  @Autowired private S3Service s3Service;

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    return ImageFileValidatorSupport.isValid(file, s3Service, context);
  }
}
