package com.sns.marigold.auth;

import com.sns.marigold.user.entity.PersonalUser;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class PersonalUserPrincipal implements OAuth2User {

  @Getter
  private final PersonalUser user;
  private final Collection<? extends GrantedAuthority> authorities;
  private Map<String, Object> attributes;
  private String nameAttributeKey;

  public PersonalUserPrincipal(PersonalUser user) {
    this.user = user;
    this.authorities =
      Collections.singletonList(
        new SimpleGrantedAuthority(user.getRole().name()));
  }

  public PersonalUserPrincipal(PersonalUser user, Map<String, Object> attributes,
    String nameAttributeKey) {
    this.user = user;
    this.attributes = attributes;
    this.nameAttributeKey = nameAttributeKey;
    this.authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));
  }

  @Override
  public String getName() {
    return user.getProviderId();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}
