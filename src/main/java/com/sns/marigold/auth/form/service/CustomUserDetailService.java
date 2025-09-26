package com.sns.marigold.auth.form.service;

import com.sns.marigold.user.entity.InstitutionUser;
import com.sns.marigold.user.service.InstitutionUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

  private final InstitutionUserService institutionUserService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    InstitutionUser user;
    try {
      user = institutionUserService.findByUsername(username);
    } catch (EntityNotFoundException e) {
      throw new UsernameNotFoundException("해당 유저를 찾을 수 없습니다");
    }
    return User.builder()
      .username(user.getEmail())
      .password(user.getPassword()) // 암호화 된 값
      .authorities(user.getRole().name())
//      .roles(user.getRole().name())
      .build();
  }
}
