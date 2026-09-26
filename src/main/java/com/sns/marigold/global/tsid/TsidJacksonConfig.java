package com.sns.marigold.global.tsid;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/*
 @RequestBody 기반의 JSON 포맷 데이터를 담당.
 DTO 필드의 @TsidId를 통해 적용.
*/
public class TsidJacksonConfig {

  /*
  Long to String
   */
  public static class Serializer extends JsonSerializer<Long> {
    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      if (value == null) {
        gen.writeNull();
      } else {
        gen.writeString(TsidCodec.encode(value));
      }
    }
  }

  /*
  String to Long
   */
  public static class Deserializer extends JsonDeserializer<Long> {
    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (!p.hasToken(JsonToken.VALUE_STRING)) {
        return ctxt.reportInputMismatch(Long.class, "TSID must be a string");
      }
      String text = p.getValueAsString();
      try {
        return TsidCodec.decode(text);
      } catch (IllegalArgumentException e) {
        throw ctxt.weirdStringException(text, Long.class, e.getMessage());
      }
    }
  }
}
