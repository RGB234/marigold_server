package com.sns.marigold.global.config;

import com.sns.marigold.global.util.HtmlCharacterEscapes;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper 설정을 통해 JSON 요청/응답 시 XSS 방어를 지원합니다.
 * @RequestBody를 사용하는 JSON 데이터 직렬화 시 HTML 이스케이프 처리를 수행합니다.
 */
@Configuration
public class XssJacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer xssEscapeCustomizer() {
        return builder -> {
            // ObjectMapper의 JsonFactory에 커스텀 CharacterEscapes를 등록합니다.
            // 이를 통해 JSON 생성 시 HTML 특수 문자가 자동으로 이스케이프됩니다.
            builder.postConfigurer(objectMapper -> 
                objectMapper.getFactory().setCharacterEscapes(new HtmlCharacterEscapes())
            );
        };
    }
}
