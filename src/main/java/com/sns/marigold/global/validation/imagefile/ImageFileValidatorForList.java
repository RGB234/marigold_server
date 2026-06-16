package com.sns.marigold.global.validation.imagefile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.storage.service.S3Service;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class ImageFileValidatorForList
    implements ConstraintValidator<ImageFile, List<MultipartFile>> {

  @Autowired private S3Service s3Service;

  @Override
  public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext ctx) {
    return ImageFileValidatorSupport.isValid(files, s3Service, ctx);
  }
}
