package com.sns.marigold.global.validator;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface ImageCountValidatable {
    List<String> getImagesToKeep();

    List<MultipartFile> getImages(); // 새로 업로드할 이미지 파일들
}
