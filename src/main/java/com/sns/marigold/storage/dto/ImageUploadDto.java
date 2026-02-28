package com.sns.marigold.storage.dto;

import com.sns.marigold.adoption.entity.AdoptionImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadDto {
  private String storedFileName;
  private String originalFileName;

  public static ImageUploadDto from(AdoptionImage image) {
    return ImageUploadDto.builder()
        .storedFileName(image.getStoredFileName())
        .originalFileName(image.getOriginalFileName())
        .build();
  }

  public static ImageUploadDto emptyDto() {
    return ImageUploadDto.builder()
        .storedFileName(null)
        .originalFileName(null)
        .build();
  }
}

