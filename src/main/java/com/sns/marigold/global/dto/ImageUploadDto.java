package com.sns.marigold.global.dto;

import com.sns.marigold.adoption.entity.AdoptionImage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadDto {
  private String imageUrl;
  private String storeFileName;
  private String originalFileName;

  public static ImageUploadDto from(AdoptionImage image) {
    return ImageUploadDto.builder()
        .imageUrl(image.getImageUrl())
        .storeFileName(image.getStoreFileName())
        .originalFileName(image.getOriginalFileName())
        .build();
  }

  public static ImageUploadDto emptyDto() {
    return ImageUploadDto.builder()
        .imageUrl(null)
        .storeFileName(null)
        .originalFileName(null)
        .build();
  }
}

