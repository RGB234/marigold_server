package com.sns.marigold.user.service;


import com.sns.marigold.user.dto.create.UserCreateDto;
import com.sns.marigold.user.dto.response.UserInfoDto;
import com.sns.marigold.user.dto.update.UserUpdateDto;
import com.sns.marigold.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface UserService {
  User findEntityById(UUID uid);

  UserInfoDto getUserById(UUID uid);

  List<UserInfoDto> getUserByNickname(String nickname);

  UUID createUser(UserCreateDto createDto);

  void updateUser(UUID uid,
                  UserUpdateDto updateDto);

  void deleteUser(UUID uid);
}