package com.sns.marigold.support;

import com.sns.marigold.storage.service.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @SuppressWarnings("resource") // 테스트 JVM 동안 공유하고 Ryuk이 종료 시 정리함.
  private static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0.32")
          .withDatabaseName("marigold_test")
          .withUsername("test")
          .withPassword("test");

  static {
    mysql.start();
  }

  @MockitoBean protected S3Service s3Service;

  @Autowired private DatabaseCleaner databaseCleaner;

  @AfterEach
  void tearDown() {
    databaseCleaner.clear();
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
  }
}
