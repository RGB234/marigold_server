package com.sns.marigold.chat.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TSIDJacksonConfig;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatMessageDto {

    private Long roomId;

    @JsonSerialize(using = TSIDJacksonConfig.Serializer.class)
    @JsonDeserialize(using = TSIDJacksonConfig.Deserializer.class)
    private Long senderId;

    private String senderNickname;
    private String message;
    private LocalDateTime createdAt;
}
