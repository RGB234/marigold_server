package com.sns.marigold.global.tsid;

import io.hypersistence.tsid.TSID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TsidCodec {

  private static final int TSID_LENGTH = 13;

  public static String encode(long value) {
    return TSID.from(value).toString();
  }

  public static long decode(String value) {
    if (value == null || value.length() != TSID_LENGTH) {
      throw new IllegalArgumentException("TSID must be 13 characters");
    }
    return TSID.from(value).toLong();
  }
}
