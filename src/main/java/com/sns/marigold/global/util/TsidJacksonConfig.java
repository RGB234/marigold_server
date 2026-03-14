package com.sns.marigold.global.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.hypersistence.tsid.TSID;
import org.springframework.util.ObjectUtils;

import java.io.IOException;


/*
@RequestBody 기반의 JSON 포맷 데이터를 담당.
DTO필드에 Serializer/Deserializer 추가해야 적용.
 */
public class TsidJacksonConfig {

  /*
  LONG to BASE32
   */
  public static class Serializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeString(TSID.from(value).toString());
      }
    }
  }

  /*
  BASE32 to LONG
   */
  public static class Deserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser p,
                            DeserializationContext ctxt) throws IOException {
      String text = p.getText();
      if (ObjectUtils.isEmpty(text)) return null;
      try {
        // 1. 먼저 TSID(Base32) 포맷으로 시도
        return TSID.from(text).toLong();
      } catch (Exception e) {
        // 2. 실패하면 일반적인 숫자(10진수)로 파싱
        try {
          return Long.parseLong(text);
        } catch (NumberFormatException nfe) {
          throw new IOException("Invalid ID format: " + text);
        }
      }
    }
  }
}

