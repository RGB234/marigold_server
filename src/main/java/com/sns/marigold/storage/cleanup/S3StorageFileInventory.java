package com.sns.marigold.storage.cleanup;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sns.marigold.storage.config.S3Properties;
import com.sns.marigold.storage.service.StorageDirectory;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
public class S3StorageFileInventory implements StorageFileInventory {

  private final S3Client s3Client;
  private final S3Properties properties;

  @Override
  public void forEachPage(Consumer<List<StoredFile>> pageConsumer) {
    String continuationToken = null;

    do {
      ListObjectsV2Response response =
          s3Client.listObjectsV2(
              ListObjectsV2Request.builder()
                  .bucket(bucketName())
                  .maxKeys(PAGE_SIZE)
                  .continuationToken(continuationToken)
                  .build());
      List<StoredFile> page =
          response.contents().stream()
              .filter(object -> StorageDirectory.isValidStoredFileName(object.key()))
              .filter(object -> object.lastModified() != null)
              .map(object -> new StoredFile(object.key(), object.lastModified()))
              .toList();
      if (!page.isEmpty()) {
        pageConsumer.accept(page);
      }
      continuationToken = response.nextContinuationToken();
    } while (continuationToken != null);
  }

  private String bucketName() {
    return Objects.requireNonNull(properties.bucket(), "bucketName must not be null");
  }
}
