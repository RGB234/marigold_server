package com.sns.marigold.auth.oauth2;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.user.entity.PersonalUser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class PersonalUserOAuth2Principal implements OAuth2User, CustomPrincipal {

  @Getter
  private final PersonalUser user;
  private final Collection<? extends GrantedAuthority> authorities;
  private Map<String, Object> attributes;
  private String nameAttributeKey;

  public PersonalUserOAuth2Principal(PersonalUser user) {
    this.user = user;
    this.authorities =
        Collections.singletonList(
            new SimpleGrantedAuthority(user.getRole().name()));
  }

  public PersonalUserOAuth2Principal(PersonalUser user, Map<String, Object> attributes,
                                     String nameAttributeKey) {
    this.user = user;
    this.attributes = attributes;
    this.nameAttributeKey = nameAttributeKey;
    this.authorities = Collections.singletonList(new SimpleGrantedAuthority(
            user.getRole().name())
//        Role.ROLE_PERSON.name())
    );
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
  public UUID getUid() {
    return user.getId();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}
