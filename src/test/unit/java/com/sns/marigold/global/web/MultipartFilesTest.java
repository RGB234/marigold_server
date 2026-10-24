package com.sns.marigold.global.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class MultipartFilesTest {

  @Test
  void nonEmptyFiltersNullAndEmptyFiles() {
    MockMultipartFile empty =
        new MockMultipartFile("images", "empty.jpg", "image/jpeg", new byte[0]);
    MockMultipartFile image =
        new MockMultipartFile("images", "image.jpg", "image/jpeg", new byte[] {1});

    assertThat(MultipartFiles.nonEmpty(Arrays.asList(null, empty, image))).containsExactly(image);
  }

  @Test
  void nonEmptyReturnsEmptyListForNull() {
    assertThat(MultipartFiles.nonEmpty(null)).isEmpty();
  }
}
