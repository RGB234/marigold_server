package com.sns.marigold.user.dto;

import com.sns.marigold.global.type.GenderType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserProfileDTO {
    @NotNull
    @Size(min=3, max=12)
    private String nickname;

    @NotNull
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생일은 yyyy-MM-dd 형식이어야 합니다.")
    private String birthday;

    @NotNull
    private GenderType gender;

    @NotNull
    private String photoURL;
}
