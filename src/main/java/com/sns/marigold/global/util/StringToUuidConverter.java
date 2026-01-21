package com.sns.marigold.global.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import java.util.UUID;

/*
* 컨트롤러에 '하이픈 없이 넘겨준 UUID 형태 문자열'을 'UUID 객체'로 변환하여 전달
*/

@Component
public class StringToUuidConverter implements Converter<String, UUID> {

    @Override
    public UUID convert(@NonNull String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        try {
            // 1. 하이픈이 없는 32자리 포맷인 경우 -> 하이픈을 삽입하여 파싱
            if (!source.contains("-") && source.length() == 32) {
                String formatted = source.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }

            // 2. 이미 하이픈이 있거나(36자) 표준 포맷인 경우
            return UUID.fromString(source);

        } catch (IllegalArgumentException e) {
            // 잘못된 포맷일 경우 400 Bad Request 등을 유발하기 위해 null 또는 예외 던짐
            throw new IllegalArgumentException("Invalid UUID format: " + source);
        }
    }
}