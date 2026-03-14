package com.sns.marigold.chat.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {
    private Long id;
    private Long postId;
    private String postTitle;
    private LocalDateTime createdAt;

    @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
    @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
    private Long user1Id;
    private String user1Nickname;

    @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
    @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
    private Long user2Id;
    private String user2Nickname;

}
