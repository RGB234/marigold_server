package com.sns.marigold.adoption.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sns.marigold.adoption.service.AdoptionPostService;
import com.sns.marigold.user.event.UserDeletedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdoptionUserDeletedListener {
  private final AdoptionPostService adoptionPostService;

  @EventListener
  public void handle(UserDeletedEvent event) {
    adoptionPostService.deleteAllByWriter(event.userId());
  }
}
