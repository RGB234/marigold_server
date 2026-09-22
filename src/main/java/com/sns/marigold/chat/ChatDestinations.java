package com.sns.marigold.chat;

import io.hypersistence.tsid.TSID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatDestinations {

  public static final String APPLICATION_PREFIX = "/pub";
  public static final String BROKER_PREFIX = "/sub";
  public static final String MESSAGE_MAPPING = "/chat/message";
  public static final String MESSAGE_SEND = APPLICATION_PREFIX + MESSAGE_MAPPING;
  public static final String ROOM_SUBSCRIPTION_PREFIX = BROKER_PREFIX + "/chat/room/";

  public static String room(Long roomId) {
    return ROOM_SUBSCRIPTION_PREFIX + TSID.from(roomId);
  }
}
