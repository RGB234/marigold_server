package com.sns.marigold.global.validation.imagefile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.service.StorageService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class ImageFileValidatorForList
    implements ConstraintValidator<ImageFile, List<MultipartFile>> {

  @Autowired private StorageService storageService;

  @Override
  public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext ctx) {
    return ImageFileValidatorSupport.isValid(files, storageService, ctx);
  }
}
