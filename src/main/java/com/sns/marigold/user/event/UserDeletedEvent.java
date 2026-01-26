package com.sns.marigold.user.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDeletedEvent {
    private final List<String> storeFileNames;
}
