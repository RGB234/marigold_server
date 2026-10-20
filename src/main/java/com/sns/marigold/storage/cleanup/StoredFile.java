package com.sns.marigold.storage.cleanup;

import java.time.Instant;

public record StoredFile(String storedFileName, Instant lastModified) {}
