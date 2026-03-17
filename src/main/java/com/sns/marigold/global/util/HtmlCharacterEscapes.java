package com.sns.marigold.global.util;

import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;

public class HtmlCharacterEscapes extends CharacterEscapes {

  private final int[] asciiEscapes;

  // 객체 생성 비용을 없애기 위해 미리 상수로 캐싱해 둡니다.
  private static final SerializedString ESCAPE_LT = new SerializedString("&lt;");
  private static final SerializedString ESCAPE_GT = new SerializedString("&gt;");
  private static final SerializedString ESCAPE_AMP = new SerializedString("&amp;");
  private static final SerializedString ESCAPE_QUOT = new SerializedString("&quot;");
  private static final SerializedString ESCAPE_APOS = new SerializedString("&#39;");

  public HtmlCharacterEscapes() {
    asciiEscapes = CharacterEscapes.standardAsciiEscapesForJSON();
    asciiEscapes['<'] = CharacterEscapes.ESCAPE_CUSTOM;
    asciiEscapes['>'] = CharacterEscapes.ESCAPE_CUSTOM;
    asciiEscapes['&'] = CharacterEscapes.ESCAPE_CUSTOM;
    asciiEscapes['\"'] = CharacterEscapes.ESCAPE_CUSTOM;
    asciiEscapes['\''] = CharacterEscapes.ESCAPE_CUSTOM;
  }

  @Override
  public int[] getEscapeCodesForAscii() {
    return asciiEscapes;
  }

  @Override
  public SerializableString getEscapeSequence(int ch) {
    return switch (ch) {
      case '<' -> ESCAPE_LT;
      case '>' -> ESCAPE_GT;
      case '&' -> ESCAPE_AMP;
      case '\"' -> ESCAPE_QUOT;
      case '\'' -> ESCAPE_APOS;
      default -> null;
    };
  }
}
