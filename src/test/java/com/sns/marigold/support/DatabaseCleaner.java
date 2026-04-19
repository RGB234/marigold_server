package com.sns.marigold.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseCleaner implements InitializingBean {

  @PersistenceContext private EntityManager entityManager;

  private List<String> tableNames;

  @Override
  public void afterPropertiesSet() {
    tableNames =
        entityManager.getMetamodel().getEntities().stream()
            .map(
                e -> {
                  String tableName = e.getName();
                  jakarta.persistence.Table tableAnnotation =
                      e.getJavaType().getAnnotation(jakarta.persistence.Table.class);
                  if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
                    tableName = tableAnnotation.name();
                  }
                  // Convert camel case to snake case if there's no Table annotation, simplistic
                  // conversion
                  return tableName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
                })
            .toList();
  }

  @Transactional
  public void clear() {
    entityManager.flush();
    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

    for (String tableName : tableNames) {
      entityManager.createNativeQuery("TRUNCATE TABLE `" + tableName + "`").executeUpdate();
    }

    entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
  }
}
