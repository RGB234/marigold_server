package com.sns.marigold.user.dto;

import com.sns.marigold.global.type.GenderType;
import com.sns.marigold.user.entity.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
public class UserProfileDTO {
    @NotBlank
    @Size(min=3, max=12)
    private String nickname;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생일은 yyyy-MM-dd 형식이어야 합니다.")
    private String birthday;

    private GenderType gender;

    @NotBlank
    private String photoURL;

    public UserEntity toUserEntity(){
        return UserEntity.builder()
            .nickname(nickname)
            .birthday(birthday)
            .gender(gender)
            .photoURL(photoURL)
            .build();
    }
}
