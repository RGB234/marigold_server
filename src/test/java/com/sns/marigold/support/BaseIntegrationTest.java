package com.sns.marigold.support;

import com.sns.marigold.storage.service.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public abstract class BaseIntegrationTest {

  @Container
  @SuppressWarnings("resource") // Testcontainers이므로 자동으로 .close() 호출됨.
  static MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0.32")
          .withDatabaseName("marigold_test")
          .withUsername("test")
          .withPassword("test");

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
    // Ensure JPA uses update or create for tests
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
  }
}
