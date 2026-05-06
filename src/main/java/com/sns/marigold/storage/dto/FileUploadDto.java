package com.sns.marigold.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FileUploadDto {
  private String storedFileName;
  private String originalFileName;
  private String contentType;
  private long fileSize;
}
