package com.sns.marigold.storage.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.service.LocalStorageService;
import com.sns.marigold.storage.service.LocalStorageService.LocalStoredFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(UrlConstants.STORAGE_BASE)
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
@RequiredArgsConstructor
public class LocalStorageController {

  private final LocalStorageService localStorageService;

  @GetMapping("/files/{domain}/{type}/{fileName:.+}")
  public ResponseEntity<Resource> viewFile(
      @PathVariable("domain") @NonNull String domain,
      @PathVariable("type") @NonNull String type,
      @PathVariable("fileName") @NonNull String fileName) {
    LocalStoredFile file = localStorageService.readFile(toStoredFileName(domain, type, fileName));
    return ResponseEntity.ok()
        .contentType(parseMediaType(file.contentType()))
        .contentLength(file.contentLength())
        .body(file.resource());
  }

  @GetMapping("/files/{domain}/{type}/{fileName:.+}/download")
  public ResponseEntity<Resource> downloadFile(
      @PathVariable("domain") @NonNull String domain,
      @PathVariable("type") @NonNull String type,
      @PathVariable("fileName") @NonNull String fileName,
      @RequestParam(name = "filename", required = false) String filename) {
    LocalStoredFile file = localStorageService.readFile(toStoredFileName(domain, type, fileName));
    ContentDisposition contentDisposition =
        ContentDisposition.attachment()
            .filename(sanitizeContentDispositionFileName(filename), StandardCharsets.UTF_8)
            .build();

    return ResponseEntity.ok()
        .contentType(parseMediaType(file.contentType()))
        .contentLength(file.contentLength())
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .body(file.resource());
  }

  private String toStoredFileName(String domain, String type, String fileName) {
    return domain + "/" + type + "/" + fileName;
  }

  private MediaType parseMediaType(String contentType) {
    try {
      return MediaType.parseMediaType(contentType);
    } catch (Exception e) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }

  private String sanitizeContentDispositionFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      return "download";
    }
    return fileName.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
  }
}
