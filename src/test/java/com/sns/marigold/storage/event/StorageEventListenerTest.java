package com.sns.marigold.storage.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sns.marigold.storage.service.StorageService;

class StorageEventListenerTest {

  private final List<String> newFiles = List.of("user/progile/new.jpg");
  private final List<String> oldFiles = List.of("user/profile/old.jpg");
  private AnnotationConfigApplicationContext context;
  private StorageService storageService;
  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getAutoCommit()).thenReturn(true);
    transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));

    storageService = mock(StorageService.class);
    context = new AnnotationConfigApplicationContext();
    context.register(Config.class);
    context.registerBean(StorageService.class, () -> storageService);
    context.registerBean(StorageEventListener.class);
    context.refresh();
  }

  @AfterEach
  void tearDown() {
    context.close();
  }

  @Test
  @DisplayName("커밋되면 기존 파일만 삭제하고 새 업로드 파일은 유지한다")
  void commitDeletesOnlyOldFiles() {
    transactionTemplate.executeWithoutResult(
        status -> {
          context.publishEvent(new DeleteUploadedStorageFilesEvent(newFiles));
          context.publishEvent(new DeleteOldStorageFilesEvent(oldFiles));
          verifyNoInteractions(storageService);
        });

    verify(storageService).deleteUploadedImagesByStoredFileNames(oldFiles);
    verifyNoMoreInteractions(storageService);
  }

  @Test
  @DisplayName("롤백되면 새 업로드 파일만 삭제하고 기존 파일은 유지한다")
  void rollbackDeletesOnlyNewFiles() {
    assertThatThrownBy(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> {
                      context.publishEvent(new DeleteUploadedStorageFilesEvent(newFiles));
                      context.publishEvent(new DeleteOldStorageFilesEvent(oldFiles));
                      verifyNoInteractions(storageService);
                      throw new IllegalStateException("DB operation failed");
                    }))
        .isInstanceOf(IllegalStateException.class);

    verify(storageService).deleteUploadedImagesByStoredFileNames(newFiles);
    verifyNoMoreInteractions(storageService);
  }

  @Test
  @DisplayName("내부 작업이 반환된 뒤 외부 트랜잭션이 롤백되어도 새 파일을 삭제한다")
  void outerRollbackDeletesNewFiles() {
    transactionTemplate.executeWithoutResult(
        outer -> {
          transactionTemplate.executeWithoutResult(
              inner -> context.publishEvent(new DeleteUploadedStorageFilesEvent(newFiles)));
          verifyNoInteractions(storageService);
          outer.setRollbackOnly();
        });

    verify(storageService).deleteUploadedImagesByStoredFileNames(newFiles);
    verifyNoMoreInteractions(storageService);
  }

  @Test
  @DisplayName("트랜잭션 본문 완료 후 커밋 직전 실패로 롤백되어도 새 파일을 삭제한다")
  void beforeCommitFailureDeletesNewFiles() {
    assertThatThrownBy(
            () ->
                transactionTemplate.executeWithoutResult(
                    status -> {
                      context.publishEvent(new DeleteUploadedStorageFilesEvent(newFiles));
                      TransactionSynchronizationManager.registerSynchronization(
                          new TransactionSynchronization() {
                            @Override
                            public void beforeCommit(boolean readOnly) {
                              throw new IllegalStateException("flush failed");
                            }
                          });
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("flush failed");

    verify(storageService).deleteUploadedImagesByStoredFileNames(newFiles);
    verifyNoMoreInteractions(storageService);
  }

  @Configuration(proxyBeanMethods = false)
  @EnableTransactionManagement
  @EnableAsync
  static class Config {

    @Bean(name = "storageTaskExecutor")
    Executor storageTaskExecutor() {
      // 이벤트 처리 결과를 결정적으로 검사한다. 트랜잭션 단계 처리는 실제 Spring 설정을 사용한다.
      return new SyncTaskExecutor();
    }
  }
}
