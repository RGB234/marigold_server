package com.sns.marigold.global.validation.imagefile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.service.StorageService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ImageFileValidatorForSingle implements ConstraintValidator<ImageFile, MultipartFile> {
  @Autowired private StorageService storageService;

  @Override
  public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
    return ImageFileValidatorSupport.isValid(file, storageService, context);
  }
}
