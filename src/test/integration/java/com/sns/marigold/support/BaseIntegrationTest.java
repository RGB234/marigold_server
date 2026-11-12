package com.sns.marigold.support;

import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

import com.sns.marigold.storage.objectstore.ObjectStorage;

@SpringBootTest
@ActiveProfiles("integration-test")
public abstract class BaseIntegrationTest {

  private static final int REDIS_PORT = 6379;

  @SuppressWarnings("resource") // 테스트 JVM 동안 공유하고 Ryuk이 종료 시 정리함.
  private static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8.0.32")
          .withDatabaseName("marigold_test")
          .withUsername("test")
          .withPassword("test");

  @SuppressWarnings("resource")
  private static final GenericContainer<?> redis =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(REDIS_PORT);

  static {
    mysql.start();
    redis.start();
  }

  @MockitoBean protected ObjectStorage objectStorage;

  @Autowired private DatabaseCleaner databaseCleaner;

  @Autowired private StringRedisTemplate redisTemplate;

  @AfterEach
  void tearDown() {
    databaseCleaner.clear();
    try (RedisConnection connection =
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
      connection.serverCommands().flushAll();
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));
  }
}
