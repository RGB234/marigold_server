package com.sns.marigold.user.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sns.marigold.global.util.TsidJacksonConfig;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.enums.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Schema(description = "사용자 공개 프로필")
@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserInfoDto {
  @Schema(description = "TSID 형식 사용자 ID", type = "string", example = "01JABCDEF1234")
  @JsonSerialize(using = TsidJacksonConfig.Serializer.class)
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private final Long id;

  @Schema(description = "표시 닉네임", example = "마리골드")
  private final String nickname;

  @Schema(description = "사용자 상태", example = "ACTIVE")
  private final UserStatus status;

  @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg", nullable = true)
  @Setter
  private String imageUrl;

  public static UserInfoDto from(User user) {
    if (user == null) return emptyDto();

    return UserInfoDto.builder()
        .id(user.getId())
        .nickname(user.getDisplayNickname())
        .status(user.getStatus())
        .imageUrl(user.getImage() != null ? user.getImage().getStoredFileName() : null)
        .build();
  }

  public static UserInfoDto emptyDto() {
    return UserInfoDto.builder().id(null).nickname(null).status(null).imageUrl(null).build();
  }
}
