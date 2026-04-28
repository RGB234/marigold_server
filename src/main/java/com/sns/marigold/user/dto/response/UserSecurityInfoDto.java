package com.sns.marigold.user.dto.response;

import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import com.sns.marigold.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserSecurityInfoDto {

  private final String email;
  private final boolean hasLocalCredentials;
  private final ProviderInfo providerInfo;
  private final boolean hasOAuth2Link;

  public static UserSecurityInfoDto from(User user) {
    return UserSecurityInfoDto.builder()
        .email(user.getEmail())
        .hasLocalCredentials(user.hasLocalCredentials())
        .providerInfo(user.getProviderInfo())
        .hasOAuth2Link(user.hasOAuth2Link())
        .build();
  }
}
