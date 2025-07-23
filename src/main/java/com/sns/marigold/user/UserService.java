package com.sns.marigold.user;

import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserSignUpDTO;
import com.sns.marigold.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

//    @Autowired
//    public UserService(UserRepository userRepository){
//        this.userRepository = userRepository;
//    }

    public String create(UserSignUpDTO userSignUpDTO){

        userRepository.save();
    }

    public String get(String id){

    }

    public String update(UserProfileDTO userProfileDTO){}

    public String delete(String id){}
}
