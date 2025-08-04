package com.sns.marigold.user.service;

import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserSignUpDTO;

public interface UserService {
    UserSignUpDTO create(UserSignUpDTO userSignUpDTO);

    UserProfileDTO getByNickname(String nickname);

    UserProfileDTO updateProfile(UserProfileDTO userProfileDTO);

    boolean softDelete(Long id);

    boolean hardDelete(Long id);

    boolean restore(Long id);
}