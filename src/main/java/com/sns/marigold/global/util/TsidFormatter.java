package com.sns.marigold.global.util;

import io.hypersistence.tsid.TSID;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

import org.springframework.format.Formatter;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

/*
Formatter의 경우 URI나 폼 데이터의 경우에 호출. WebConfig로 등록하여 전역으로 적용.
@RequestBody 기반의 JSON 데이터에는 TsidJacksonConfig 담당.
 */
public class TsidFormatter implements Formatter<Long> {

  @Override
  @NonNull
  public Long parse(@NonNull String text, @NonNull Locale locale) throws ParseException {
    if (!StringUtils.hasText(text)) {
      throw new ParseException("Empty TSID string", 0);
    }
    // Base32 String -> Long 변환
    try {
      return TSID.from(text).toLong();
    } catch (Exception e) {
      throw new ParseException("Invalid TSID format: " + text, 0);
    }
  }

  @Override
  @NonNull
  public String print(@NonNull Long object, @NonNull Locale locale) {
    // Long -> Base32 String 변환 (응답 시 활용)
    return Objects.requireNonNull(TSID.from(object).toLowerCase());
  }
}
