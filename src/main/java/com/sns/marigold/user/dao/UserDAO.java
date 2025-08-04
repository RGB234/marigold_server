package com.sns.marigold.user.dao;

import com.sns.marigold.user.entity.UserEntity;
import java.util.Optional;

public interface UserDAO {
    Optional<UserEntity> getById(Long id);
    Optional<UserEntity> getByNickname(String nickname);
    UserEntity save(UserEntity userEntity);
    UserEntity update(UserEntity userEntity);
    boolean softDeleteById(Long id);
    boolean hardDeleteById(Long id);
    boolean restoreById(Long id);
}
