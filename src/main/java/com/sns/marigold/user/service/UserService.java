package com.sns.marigold.user.service;

import com.sns.marigold.user.dto.UserCreateDTO;
import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserUpdateDTO;

public interface UserService {

  UserProfileDTO create(UserCreateDTO userSignUpDTO);

  UserProfileDTO update(Long id, UserUpdateDTO userUpdateProfileDTO);

  UserProfileDTO get(String username);

  boolean softDelete(Long id);

  void hardDelete(Long id);

  boolean restore(Long id);
}
