package com.sns.marigold.auth.form.service;

import com.sns.marigold.auth.form.CustomUserDetails;
import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.service.InstitutionUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final InstitutionUserService institutionUserService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    InstitutionUser user;
    try {
      user = institutionUserService.findByUsername(username);
    } catch (EntityNotFoundException e) {
      throw new UsernameNotFoundException("해당 유저를 찾을 수 없습니다");
    }

    return CustomUserDetails.builder()
        .uid(user.getId())
        .username(user.getUsername())
        .password(user.getPassword()) // 암호화된 pw
        .authorities(Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())))
        .build();
  }
}