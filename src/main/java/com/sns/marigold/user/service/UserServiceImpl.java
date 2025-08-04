package com.sns.marigold.user.service;

import com.sns.marigold.user.dao.UserDAO;
import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserSignUpDTO;
import com.sns.marigold.user.entity.UserEntity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Override
    public UserSignUpDTO create(@RequestBody @Valid UserSignUpDTO userSignUpDTO) {
        logger.info("req body : {}", userSignUpDTO);
        UserEntity userEntity = userSignUpDTO.toUserEntity();
        userDAO.save(userEntity);
        return userSignUpDTO;
    }

    @Override
    public UserProfileDTO getByNickname(String nickname) {
        UserEntity userEntity = userDAO.getByNickname(nickname)
            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저 ID"));
        return userEntity.toUserProfileDTO();
    }

    @Transactional
    @Override
    public UserProfileDTO updateProfile(UserProfileDTO userProfileDTO) {
        UserEntity userEntity = userDAO.getByNickname(userProfileDTO.getNickname())
            .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 유저 닉네임"));

        userEntity.setBirthday(userProfileDTO.getBirthday());
        userEntity.setGender(userProfileDTO.getGender());
        userEntity.setPhotoURL(userProfileDTO.getPhotoURL());
        userEntity.setNickname(userProfileDTO.getNickname());
        userDAO.save(userEntity);
        return userProfileDTO;
    }

    @Override
    public boolean softDelete(Long id) {
        return false;
    }

    @Override
    public boolean hardDelete(Long id) {
        return userDAO.hardDeleteById(id);
    }

    @Override
    public boolean restore(Long id) {
        return false;
    }
}
