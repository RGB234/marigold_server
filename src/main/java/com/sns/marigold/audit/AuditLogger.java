package com.sns.marigold.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

  private static final Logger audit = LoggerFactory.getLogger("audit");

  public void info(String message, Object... args) {
    audit.info(message, args);
  }

  public void warn(String message, Object... args) {
    audit.warn(message, args);
  }
}
