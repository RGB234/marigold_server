package com.sns.marigold.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
백엔드 Java 상수와 JSON 일치 검사
 */
class ValidationPolicyContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void validationPolicyJsonMatchesJavaConstants() throws Exception {
    Map<String, Object> expected =
        objectMapper.readValue(
            objectMapper.writeValueAsString(ValidationPolicy.contract()), new TypeReference<>() {});

    try (InputStream inputStream = getClass().getResourceAsStream("/validation-policy.json")) {
      Map<String, Object> actual = objectMapper.readValue(inputStream, new TypeReference<>() {});

      assertThat(actual).isEqualTo(expected);
    }
  }
}
