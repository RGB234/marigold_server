package com.sns.marigold.chat;

import com.sns.marigold.global.tsid.TsidCodec;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatDestinations {

  public static final String APPLICATION_PREFIX = "/pub";
  public static final String BROKER_PREFIX = "/sub";
  public static final String QUEUE_PREFIX = "/queue";
  public static final String USER_DESTINATION_PREFIX = "/user";
  public static final String MESSAGE_MAPPING = "/chat/message";
  public static final String MESSAGE_SEND = APPLICATION_PREFIX + MESSAGE_MAPPING;
  public static final String ROOM_SUBSCRIPTION_PREFIX = BROKER_PREFIX + "/chat/room/";
  public static final String ERROR_QUEUE = QUEUE_PREFIX + "/errors";
  public static final String USER_ERROR_QUEUE = USER_DESTINATION_PREFIX + ERROR_QUEUE;

  public static String room(Long roomId) {
    return ROOM_SUBSCRIPTION_PREFIX + TsidCodec.encode(roomId);
  }
}
