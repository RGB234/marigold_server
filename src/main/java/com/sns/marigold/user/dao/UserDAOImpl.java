package com.sns.marigold.user.dao;

import com.sns.marigold.user.entity.UserEntity;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDAOImpl implements UserDAO{
    private final UserRepository userRepository;

    @Override
    public Optional<UserEntity> getById(Long id){
        return userRepository.findById(id);
    }

    @Override
    public Optional<UserEntity> getByNickname(String nickname){
        return userRepository.findByNickname(nickname);
    }

    @Override
    public UserEntity save(UserEntity userEntity){
        userRepository.save(userEntity);
        return userEntity;
    }

    @Override
    public UserEntity update(UserEntity userEntity) {
        userRepository.save(userEntity);
        return userEntity;
    }

    @Override
    public boolean softDeleteById(Long id){
        return false;
    }

    @Override
    public boolean hardDeleteById(Long id){
        UserEntity userEntity = getById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저 ID"));
        userRepository.delete(userEntity);
        return true;
    }

    @Override
    public boolean restoreById(Long id){
        return false;
    }
}
