package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sns.marigold.storage.config.S3Properties;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@ExtendWith(MockitoExtension.class)
class S3StorageFileInventoryTest {

  @Mock private S3Client s3Client;

  @Test
  void passesEachS3PageAndIgnoresUnmanagedKeys() {
    Instant firstModified = Instant.parse("2026-10-01T00:00:00Z");
    Instant secondModified = Instant.parse("2026-10-02T00:00:00Z");
    String firstKey = "user/profile/11111111-1111-1111-1111-111111111111.jpg";
    String secondKey = "adoption/post/22222222-2222-2222-2222-222222222222.jpg";
    ListObjectsV2Response firstPage =
        ListObjectsV2Response.builder()
            .contents(
                S3Object.builder().key(firstKey).lastModified(firstModified).build(),
                S3Object.builder().key("unmanaged.txt").lastModified(firstModified).build())
            .nextContinuationToken("next")
            .build();
    ListObjectsV2Response unmanagedOnlyPage =
        ListObjectsV2Response.builder()
            .contents(S3Object.builder().key("unmanaged.txt").lastModified(firstModified).build())
            .nextContinuationToken("last")
            .build();
    ListObjectsV2Response lastPage =
        ListObjectsV2Response.builder()
            .contents(S3Object.builder().key(secondKey).lastModified(secondModified).build())
            .build();
    given(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
        .willReturn(firstPage, unmanagedOnlyPage, lastPage);
    S3StorageFileInventory inventory =
        new S3StorageFileInventory(s3Client, new S3Properties("test-bucket"));

    List<List<StoredFile>> pages = new ArrayList<>();
    inventory.forEachPage(pages::add);

    assertThat(pages)
        .containsExactly(
            List.of(new StoredFile(firstKey, firstModified)),
            List.of(new StoredFile(secondKey, secondModified)));
    ArgumentCaptor<ListObjectsV2Request> requestCaptor =
        ArgumentCaptor.forClass(ListObjectsV2Request.class);
    verify(s3Client, times(3)).listObjectsV2(requestCaptor.capture());
    List<ListObjectsV2Request> requests = requestCaptor.getAllValues();
    assertThat(requests)
        .allSatisfy(
            request -> assertThat(request.maxKeys()).isEqualTo(StorageFileInventory.PAGE_SIZE));
    assertThat(requests.get(0).continuationToken()).isNull();
    assertThat(requests.get(1).continuationToken()).isEqualTo("next");
    assertThat(requests.get(2).continuationToken()).isEqualTo("last");
  }
}
