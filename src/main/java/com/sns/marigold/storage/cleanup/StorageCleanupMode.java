package com.sns.marigold.storage.cleanup;

public enum StorageCleanupMode {
  OFF,
  SCHEDULED,
  STARTUP,
  BOTH;

  public boolean runsOnSchedule() {
    return this == SCHEDULED || this == BOTH;
  }

  public boolean runsOnStartup() {
    return this == STARTUP || this == BOTH;
  }
}
