package com.sns.marigold.adoption.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionWithChatDto {
    private AdoptionInfoDto adoptionInfo;
    
    private Long chatRoomId;
    
    @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
    @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
    private Long receiverId;
    
    private String receiverNickname;
    
    private LocalDateTime chatCreatedAt;
}
