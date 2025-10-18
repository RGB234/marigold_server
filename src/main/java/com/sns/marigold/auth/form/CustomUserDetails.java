package com.sns.marigold.auth.form;

import com.sns.marigold.auth.common.CustomPrincipal;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;


@Builder
public class CustomUserDetails implements UserDetails, CustomPrincipal {
  @Getter
  private UUID uid;
  private String username;
  private String password;
  private Collection<SimpleGrantedAuthority> authorities;

  private CustomUserDetails(UUID uid, String username, String password, Collection<SimpleGrantedAuthority> authorities) {
    this.uid = uid;
    this.username = username;
    this.password = password;
    this.authorities = authorities;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

}
