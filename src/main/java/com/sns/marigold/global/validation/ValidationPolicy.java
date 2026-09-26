package com.sns.marigold.global.validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ValidationPolicy {

  public static final int VERSION = 1;

  private ValidationPolicy() {}

  public static final class Email {
    public static final String PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private Email() {}
  }

  public static final class Password {
    public static final int MIN_LENGTH = 8;

    private Password() {}
  }

  public static final class User {
    public static final int NICKNAME_MIN_LENGTH = 2;
    public static final int NICKNAME_MAX_LENGTH = 12;
    public static final String NICKNAME_ALLOWED_PATTERN = "^[a-zA-Z가-힣0-9]+$";

    private User() {}
  }

  public static final class AdoptionPost {
    public static final int AGE_MIN = 0;
    public static final int WEIGHT_MIN = 0;
    public static final int TITLE_MAX_LENGTH = 16;
    public static final int FEATURES_MIN_LENGTH = 20;
    public static final int FEATURES_MAX_LENGTH = 500;
    public static final ImageCountPolicy IMAGE_COUNT = new ImageCountPolicy(1, 8);

    private AdoptionPost() {}
  }

  public static final class Comment {
    public static final int CONTENT_MAX_LENGTH = 1000;
    public static final ImageCountPolicy IMAGE_COUNT = new ImageCountPolicy(0, 1);

    private Comment() {}
  }

  public static final class Image {
    public static final int MAX_SIZE_MB = 5;
    public static final long MAX_SIZE_BYTES = MAX_SIZE_MB * 1024L * 1024L;
    public static final Map<String, List<String>> ALLOWED_MIME_TYPES_BY_EXTENSION =
        createAllowedMimeTypesByExtension();
    public static final List<String> ALLOWED_MIME_TYPES =
        ALLOWED_MIME_TYPES_BY_EXTENSION.values().stream().flatMap(List::stream).distinct().toList();
    public static final List<String> ALLOWED_EXTENSIONS =
        List.copyOf(ALLOWED_MIME_TYPES_BY_EXTENSION.keySet());

    private Image() {}

    private static Map<String, List<String>> createAllowedMimeTypesByExtension() {
      Map<String, List<String>> mimeTypes = new LinkedHashMap<>();
      mimeTypes.put("jpg", List.of("image/jpeg"));
      mimeTypes.put("jpeg", List.of("image/jpeg"));
      mimeTypes.put("png", List.of("image/png"));
      mimeTypes.put("webp", List.of("image/webp"));
      return Collections.unmodifiableMap(mimeTypes);
    }
  }

  public static final class ChatAttachment {
    public static final int MAX_COUNT = 6;
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    public static final long MAX_TOTAL_SIZE_BYTES = MAX_FILE_SIZE_BYTES * MAX_COUNT;
    public static final Map<String, List<String>> ALLOWED_MIME_TYPES_BY_EXTENSION =
        Map.ofEntries(
            Map.entry("jpg", List.of("image/jpeg")),
            Map.entry("jpeg", List.of("image/jpeg")),
            Map.entry("png", List.of("image/png")),
            Map.entry("webp", List.of("image/webp")),
            Map.entry("pdf", List.of("application/pdf")),
            Map.entry(
                "docx",
                List.of(
                    "application/x-tika-ooxml",
                    "application/zip",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(
                "xlsx",
                List.of(
                    "application/x-tika-ooxml",
                    "application/zip",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(
                "pptx",
                List.of(
                    "application/x-tika-ooxml",
                    "application/zip",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("txt", List.of("text/plain")),
            Map.entry("csv", List.of("text/csv", "text/plain", "application/csv")),
            Map.entry("hwpx", List.of("application/zip", "application/vnd.hancom.hwpx")));

    public static final List<String> ALLOWED_EXTENSIONS =
        ALLOWED_MIME_TYPES_BY_EXTENSION.keySet().stream().sorted().toList();

    private ChatAttachment() {}
  }

  public static final class Tsid {
    public static final int BASE32_LENGTH = 13;
    public static final String BASE32_PATTERN = "^[0-9ABCDEFGHJKMNPQRSTVWXYZ]{13}$";

    private Tsid() {}
  }

  public static Map<String, Object> contract() {
    return Map.ofEntries(
        Map.entry("version", VERSION),
        Map.entry("email", Map.of("pattern", Email.PATTERN)),
        Map.entry("password", Map.of("minLength", Password.MIN_LENGTH)),
        Map.entry(
            "user",
            Map.of(
                "nickname",
                Map.of(
                    "minLength",
                    User.NICKNAME_MIN_LENGTH,
                    "maxLength",
                    User.NICKNAME_MAX_LENGTH,
                    "allowedPattern",
                    User.NICKNAME_ALLOWED_PATTERN))),
        Map.entry(
            "adoptionPost",
            Map.of(
                "age",
                Map.of("min", AdoptionPost.AGE_MIN),
                "weight",
                Map.of("min", AdoptionPost.WEIGHT_MIN),
                "title",
                Map.of("maxLength", AdoptionPost.TITLE_MAX_LENGTH),
                "features",
                Map.of(
                    "minLength",
                    AdoptionPost.FEATURES_MIN_LENGTH,
                    "maxLength",
                    AdoptionPost.FEATURES_MAX_LENGTH),
                "images",
                Map.of(
                    "minCount", AdoptionPost.IMAGE_COUNT.min(),
                    "maxCount", AdoptionPost.IMAGE_COUNT.max()))),
        Map.entry(
            "comment",
            Map.of(
                "content",
                Map.of("maxLength", Comment.CONTENT_MAX_LENGTH),
                "images",
                Map.of(
                    "minCount", Comment.IMAGE_COUNT.min(),
                    "maxCount", Comment.IMAGE_COUNT.max()))),
        Map.entry(
            "image",
            Map.of(
                "maxSizeMb",
                Image.MAX_SIZE_MB,
                "maxSizeBytes",
                Image.MAX_SIZE_BYTES,
                "allowedMimeTypes",
                Image.ALLOWED_MIME_TYPES,
                "allowedExtensions",
                Image.ALLOWED_EXTENSIONS)),
        Map.entry(
            "chatAttachment",
            Map.of(
                "maxCount",
                ChatAttachment.MAX_COUNT,
                "maxFileSizeBytes",
                ChatAttachment.MAX_FILE_SIZE_BYTES,
                "maxTotalSizeBytes",
                ChatAttachment.MAX_TOTAL_SIZE_BYTES,
                "allowedExtensions",
                ChatAttachment.ALLOWED_EXTENSIONS,
                "allowedMimeTypesByExtension",
                ChatAttachment.ALLOWED_MIME_TYPES_BY_EXTENSION)),
        Map.entry(
            "tsid",
            Map.of("base32Length", Tsid.BASE32_LENGTH, "base32Pattern", Tsid.BASE32_PATTERN)));
  }
}
