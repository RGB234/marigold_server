package com.sns.marigold.global.tsid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class TsidCodecTest {

  @Test
  void encodesAsUppercaseAndDecodesBackToLong() {
    long value = 991000000000000001L;

    String encoded = TsidCodec.encode(value);

    assertThat(encoded).isEqualTo(encoded.toUpperCase(Locale.ROOT));
    assertThat(TsidCodec.decode(encoded)).isEqualTo(value);
  }

  @Test
  void rejectsDecimalFallback() {
    assertThatThrownBy(() -> TsidCodec.decode("123")).isInstanceOf(IllegalArgumentException.class);
  }
}
