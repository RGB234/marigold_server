package com.sns.marigold.global.util;

import com.sns.marigold.global.annotation.TsidType;
import java.util.HashSet;
import java.util.Set;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.lang.NonNull;

/*
 TsidType 어노테이션이 붙으면 자동으로 TsidFormatter 호출하여 처리. WebConfig에 등록.
*/
public class TsidAnnotationFormatterFactory implements AnnotationFormatterFactory<TsidType> {

  @Override
  @NonNull
  public Set<Class<?>> getFieldTypes() {
    // 이 팩토리가 처리할 타겟 타입은 Long
    Set<Class<?>> fieldTypes = new HashSet<>();
    fieldTypes.add(Long.class);
    return fieldTypes;
  }

  @Override
  @NonNull
  public Printer<?> getPrinter(@NonNull TsidType annotation, @NonNull Class<?> fieldType) {
    return new TsidFormatter();
  }

  @Override
  @NonNull
  public Parser<?> getParser(@NonNull TsidType annotation, @NonNull Class<?> fieldType) {
    return new TsidFormatter();
  }
}
