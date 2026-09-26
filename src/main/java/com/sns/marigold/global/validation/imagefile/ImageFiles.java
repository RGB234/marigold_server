package com.sns.marigold.global.validation.imagefile;

import java.util.List;
import java.util.Objects;

import org.springframework.web.multipart.MultipartFile;

public final class ImageFiles {

  private ImageFiles() {}

  public static List<MultipartFile> nonEmpty(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      return List.of();
    }

    return files.stream().filter(Objects::nonNull).filter(file -> !file.isEmpty()).toList();
  }
}
