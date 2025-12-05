package com.sns.marigold.global.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadDto {
  private String imageUrl;
  private String storeFileName;
  private String originalFileName;
}

