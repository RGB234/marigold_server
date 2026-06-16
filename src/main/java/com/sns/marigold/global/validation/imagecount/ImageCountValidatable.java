package com.sns.marigold.global.validation.imagecount;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/*
  ImageCount를 어노테이션을 적용하려는 DTO가 구현해야 할 인터페이스
 */
public interface ImageCountValidatable {
  List<String> getImagesToKeep(); // Update DTO에서 유지할 이미지 파일목록

  List<MultipartFile> getImages(); // 새로 업로드할 이미지 파일목록
}