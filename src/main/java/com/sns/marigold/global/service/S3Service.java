package com.sns.marigold.global.service;

import com.sns.marigold.global.dto.ImageUploadDto;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

  private final S3Template s3Template;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  public ImageUploadDto uploadFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String key = UUID.randomUUID().toString() + extension;

    try (InputStream inputStream = file.getInputStream()) {
      s3Template.upload(bucketName, key, inputStream,
          ObjectMetadata.builder().contentType(file.getContentType()).build());
    } catch (IOException e) {
      log.error("S3 upload failed", e);
      throw new RuntimeException("File upload failed", e);
    }

    // 업로드된 URL 반환
    try {
        // 실제 다운로드가 아니라, 접근 가능한 "링크"만 생성합니다. (가볍고 빠름)
        // Duration.ofMinutes(10): 10분 동안만 유효한 링크
        URL url = s3Template.createSignedGetURL(bucketName, key, Duration.ofMinutes(10));
        
        return ImageUploadDto.builder()
            .imageUrl(url.toString())
            .storeFileName(key)
            .originalFileName(originalFilename)
            .build();
    } catch (Exception e) {
        log.error("S3 URL 생성 실패: {}", e.getMessage());
        throw new RuntimeException("파일 URL을 가져오는데 실패했습니다.", e);
    }
  }
}