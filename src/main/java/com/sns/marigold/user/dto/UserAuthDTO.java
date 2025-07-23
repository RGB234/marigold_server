package com.sns.marigold.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserAuthDTO {
    @NotNull
    @Email
    private String email;

    @NotNull
    @Size(min=8, max=20)
    private String password;
}
