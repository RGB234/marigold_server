package com.sns.marigold.global.service;

import com.sns.marigold.global.dto.ImageUploadDto;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

  private final S3Template s3Template;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Value("${spring.cloud.aws.region.static}")
  private String region;

  public ImageUploadDto uploadFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 없습니다.");
    }

    String originalFilename = file.getOriginalFilename();
    String key = createFileName(originalFilename); // Random UUID + 파일 확장자
    Objects.requireNonNull(key, "key must not be null");

    String safeBucketName = Objects.requireNonNull(bucketName, "bucketName must not be null");

    try (InputStream inputStream = file.getInputStream()) {
      s3Template.upload(safeBucketName, key, inputStream,
          ObjectMetadata.builder().contentType(file.getContentType()).contentLength(file.getSize()).build());
    } catch (IOException e) {
      log.error("S3 upload failed", e);
      throw new RuntimeException("File upload failed", e);
    }

    // 업로드된 URL 반환 (상시 접근 가능한 공개 URL)
    // S3 버킷이 공개 읽기 권한을 가져야 합니다.
    String publicUrl = buildPublicUrl(safeBucketName, key);
    
    return ImageUploadDto.builder()
        .imageUrl(publicUrl)
        .storeFileName(key)
        .originalFileName(originalFilename)
        .build();
  }

  public void deleteFile(String storeFileName){
    Objects.requireNonNull(storeFileName, "storeFileName must not be null");
    String safeBucketName = Objects.requireNonNull(bucketName, "bucketName must not be null");
    s3Template.deleteObject(safeBucketName, storeFileName);
  }

  public List<ImageUploadDto> uploadImagesToS3(List<MultipartFile> images) {
    List<ImageUploadDto> result = new ArrayList<>();
    try {
      for (MultipartFile image : images) {
        // uploadFile 내부에서 실패 시 예외를 던지도록 설계 권장
        result.add(this.uploadFile(image));
      }
    } catch (Exception e) {
      // 업로드 중간에 실패하면, 이미 올라간 파일들 삭제 후 예외 발생
      this.deleteUploadedImagesFromS3(result);
      throw e;
    }
    return result;
  }

  public void deleteUploadedImagesFromS3(List<ImageUploadDto> images) {
    for (ImageUploadDto dto : images) {
      this.deleteFile(dto.getStoreFileName());
    }
  }

  /**
   * S3 공개 URL 생성
   * @param bucketName S3 버킷 이름
   * @param key 파일 키 (경로)
   * @return 상시 접근 가능한 공개 URL
   */
  private String buildPublicUrl(String bucketName, String key) {
    // S3 공개 URL 형식: https://{bucket}.s3.{region}.amazonaws.com/{key}
    return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
  }

  private String createFileName(String fileName){
    return UUID.randomUUID().toString().concat(getFileExtension(fileName));
  }

  private String getFileExtension(String fileName){
    if(fileName.lastIndexOf(".") == -1){
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 형식의 파일" + fileName + ") 입니다.");
    }
    return fileName.substring(fileName.lastIndexOf("."));
  }
}