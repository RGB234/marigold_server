package com.sns.marigold.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sns.marigold.audit.AuditLogger;
import com.sns.marigold.global.error.GlobalExceptionHandler;

@SpringBootTest(
    classes = MultipartUploadLimitTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.address=127.0.0.1")
class MultipartUploadLimitTest {

  private static final int FIVE_MIB = 5 * 1024 * 1024;

  @Autowired private TestRestTemplate restTemplate;
  @MockitoBean private AuditLogger auditLogger;

  @Test
  void eightFilesAtFiveMiB_AreAcceptedByServlet() throws Exception {
    ResponseEntity<Map> response = upload(8, FIVE_MIB, false);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().get("size")).isEqualTo(8 * FIVE_MIB);
  }

  @Test
  void fileOverFiveMiB_ReturnsPayloadTooLarge() throws Exception {
    assertPayloadTooLarge(upload(1, FIVE_MIB + 1, false));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void requestOverFiftyMiB_ReturnsPayloadTooLarge(boolean withContentLength) throws Exception {
    assertPayloadTooLarge(upload(11, FIVE_MIB, withContentLength));
  }

  private void assertPayloadTooLarge(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody().get("status")).isEqualTo(413);
    assertThat(response.getBody().get("errorCode")).isEqualTo("FILE_TOO_LARGE");
  }

  private ResponseEntity<Map> upload(int fileCount, int fileSize, boolean withContentLength)
      throws Exception {
    ByteArrayResource file =
        new ByteArrayResource(new byte[fileSize]) {
          @Override
          public String getFilename() {
            return "upload.bin";
          }
        };
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    for (int i = 0; i < fileCount; i++) {
      body.add("files", file);
    }
    if (withContentLength) {
      MockHttpOutputMessage message = new MockHttpOutputMessage();
      new FormHttpMessageConverter().write(body, MediaType.MULTIPART_FORM_DATA, message);
      return restTemplate.postForEntity(
          "/upload", new HttpEntity<>(message.getBodyAsBytes(), message.getHeaders()), Map.class);
    }
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    return restTemplate.postForEntity("/upload", new HttpEntity<>(body, headers), Map.class);
  }

  @Configuration(proxyBeanMethods = false)
  @ImportAutoConfiguration({
    ServletWebServerFactoryAutoConfiguration.class,
    EmbeddedWebServerFactoryCustomizerAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    MultipartAutoConfiguration.class,
    JacksonAutoConfiguration.class
  })
  @Import({UploadController.class, GlobalExceptionHandler.class})
  static class Config {}

  @RestController
  static class UploadController {
    @PostMapping("/upload")
    Map<String, Long> upload(@RequestParam("files") List<MultipartFile> files) {
      return Map.of("size", files.stream().mapToLong(MultipartFile::getSize).sum());
    }
  }
}
