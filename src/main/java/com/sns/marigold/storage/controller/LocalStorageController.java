package com.sns.marigold.storage.controller;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.auth.exception.AuthException;
import com.sns.marigold.global.web.UrlConstants;
import com.sns.marigold.storage.objectstore.LocalObjectStorage;
import com.sns.marigold.storage.objectstore.LocalObjectStorage.LocalStoredFile;
import com.sns.marigold.storage.objectstore.LocalStorageUrlSigner;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(UrlConstants.STORAGE_BASE)
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
@RequiredArgsConstructor
public class LocalStorageController {

  private static final String GET = "GET";
  private static final String PUT = "PUT";

  private final LocalObjectStorage localObjectStorage;
  private final LocalStorageUrlSigner localStorageUrlSigner;

  @GetMapping("/files/{domain}/{type}/{fileName:.+}")
  public ResponseEntity<Resource> viewFile(
      @PathVariable("domain") @NonNull String domain,
      @PathVariable("type") @NonNull String type,
      @PathVariable("fileName") @NonNull String fileName,
      @RequestParam(name = "expires", required = false) Long expires,
      @RequestParam(name = "signature", required = false) String signature) {
    String storedFileName = toStoredFileName(domain, type, fileName);
    validateSignature(toViewPath(storedFileName), expires, null, signature);

    LocalStoredFile file = localObjectStorage.readFile(storedFileName);
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
      @RequestParam(name = "filename", required = false) String filename,
      @RequestParam(name = "expires", required = false) Long expires,
      @RequestParam(name = "signature", required = false) String signature) {
    String storedFileName = toStoredFileName(domain, type, fileName);
    String sanitizedFileName = sanitizeContentDispositionFileName(filename);
    validateSignature(toDownloadPath(storedFileName), expires, sanitizedFileName, signature);

    LocalStoredFile file = localObjectStorage.readFile(storedFileName);
    ContentDisposition contentDisposition =
        ContentDisposition.attachment().filename(sanitizedFileName, StandardCharsets.UTF_8).build();

    return ResponseEntity.ok()
        .contentType(parseMediaType(file.contentType()))
        .contentLength(file.contentLength())
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
        .body(file.resource());
  }

  // S3 presigned PUT과 같은 역할. 인증 대신 발급 시 서명한 URL로만 쓸 수 있다.
  @PutMapping("/files/{domain}/{type}/{fileName:.+}")
  public ResponseEntity<Void> uploadFile(
      @PathVariable("domain") @NonNull String domain,
      @PathVariable("type") @NonNull String type,
      @PathVariable("fileName") @NonNull String fileName,
      @RequestParam(name = "expires", required = false) Long expires,
      @RequestParam(name = "signature", required = false) String signature,
      HttpServletRequest request)
      throws IOException {
    String storedFileName = toStoredFileName(domain, type, fileName);
    validateSignature(PUT, toViewPath(storedFileName), expires, null, signature);

    localObjectStorage.storeUploadedFile(
        storedFileName, request.getInputStream(), request.getContentLengthLong());
    return ResponseEntity.ok().build();
  }

  private String toStoredFileName(String domain, String type, String fileName) {
    return domain + "/" + type + "/" + fileName;
  }

  private void validateSignature(String path, Long expires, String filename, String signature) {
    validateSignature(GET, path, expires, filename, signature);
  }

  private void validateSignature(
      String method, String path, Long expires, String filename, String signature) {
    if (!localStorageUrlSigner.isValid(method, path, expires, filename, signature)) {
      throw AuthException.forAccessDenied();
    }
  }

  private String toViewPath(String storedFileName) {
    return UrlConstants.STORAGE_BASE + "/files/" + storedFileName;
  }

  private String toDownloadPath(String storedFileName) {
    return toViewPath(storedFileName) + "/download";
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
