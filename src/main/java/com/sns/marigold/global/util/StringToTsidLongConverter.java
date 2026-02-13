package com.sns.marigold.global.util;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import io.hypersistence.tsid.TSID;

/*
컨트롤러에 전달된 String format의 TSID를 Long 타입으로 변환하여 전달하는 컨버터
 */
@Component
public class StringToTsidLongConverter implements Converter<String, Long> {

    @Override
    public Long convert(@NonNull String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        try {
            return TSID.from(source).toLong();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid TSID format: " + source);
        }
    }
}