package com.sns.marigold.global.util;

import io.hypersistence.tsid.TSID;
import java.text.ParseException;
import java.util.Locale;
import org.springframework.format.Formatter;
import org.springframework.util.StringUtils;

/*
Formatter의 경우 URI나 폼 데이터의 경우에 호출. WebConfig로 등록하여 전역으로 적용.
@RequestBody 기반의 JSON 데이터에는 TsidJacksonConfig 담당.
 */
public class TsidFormatter implements Formatter<Long> {

  @Override
  public Long parse(String text, Locale locale) throws ParseException {
    if (!StringUtils.hasText(text)) {
      return null;
    }
    // Base32 String -> Long 변환
    return TSID.from(text).toLong();
  }

  @Override
  public String print(Long object, Locale locale) {
    if (object == null) {
      return "";
    }
    // Long -> Base32 String 변환 (응답 시 활용)
    return TSID.from(object).toLowerCase();
  }
}