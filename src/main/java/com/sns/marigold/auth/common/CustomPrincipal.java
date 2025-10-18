package com.sns.marigold.auth.common;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

public interface CustomPrincipal {
  public UUID getUid();

  public Collection<? extends GrantedAuthority> getAuthorities();
}
