package com.sns.marigold.global.util;

import com.sns.marigold.global.annotation.TsidType;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;

import java.util.HashSet;
import java.util.Set;

/*
  TsidType 어노테이션이 붙으면 자동으로 TsidFormatter 호출하여 처리. WebConfig에 등록.
 */
public class TsidAnnotationFormatterFactory implements AnnotationFormatterFactory<TsidType> {

  @Override
  public Set<Class<?>> getFieldTypes() {
    // 이 팩토리가 처리할 타겟 타입은 Long
    Set<Class<?>> fieldTypes = new HashSet<>();
    fieldTypes.add(Long.class);
    return fieldTypes;
  }

  @Override
  public Printer<?> getPrinter(TsidType annotation, Class<?> fieldType) {
    return new TsidFormatter();
  }

  @Override
  public Parser<?> getParser(TsidType annotation, Class<?> fieldType) {
    return new TsidFormatter();
  }
}