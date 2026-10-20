package com.sns.marigold.storage.cleanup;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.storage.cleanup")
public record StorageCleanupProperties(
    @DefaultValue("off") StorageCleanupMode mode,
    @DefaultValue("0 0 4 * * *") String cron,
    @DefaultValue("Asia/Seoul") String zone,
    @DefaultValue("24h") Duration minimumAge,
    @DefaultValue("true") boolean dryRun) {

  public StorageCleanupProperties {
    Objects.requireNonNull(mode, "storage cleanup mode must not be null");
    Objects.requireNonNull(cron, "storage cleanup cron must not be null");
    Objects.requireNonNull(zone, "storage cleanup zone must not be null");
    Objects.requireNonNull(minimumAge, "storage cleanup minimum age must not be null");
    if (minimumAge.isZero() || minimumAge.isNegative()) {
      throw new IllegalArgumentException("storage cleanup minimum age must be positive");
    }
  }
}
