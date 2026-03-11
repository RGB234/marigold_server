package com.sns.marigold.chat.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private Long id;
    private Long user1Id;
    private String user1Nickname;
    private Long user2Id;
    private String user2Nickname;
    private LocalDateTime createdAt;
}
