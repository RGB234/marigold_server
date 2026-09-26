package com.sns.marigold.global.tsid;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

public final class TsidValueConverter implements Converter<String, TsidValue> {

  @Override
  public TsidValue convert(@NonNull String source) {
    return new TsidValue(TsidCodec.decode(source));
  }
}
