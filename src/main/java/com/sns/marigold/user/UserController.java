package com.sns.marigold.user;

import com.sns.marigold.user.dto.UserProfileDTO;
import com.sns.marigold.user.dto.UserSignUpDTO;
import com.sns.marigold.user.service.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

//@Controller("/user")
@RestController("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserServiceImpl userService;

    @PostMapping("/create")
    public UserSignUpDTO create(@RequestBody @Valid UserSignUpDTO userSignUpDTO){
        return userService.create(userSignUpDTO);
    }

    @GetMapping("/{nickname}")
    public UserProfileDTO getByNickname(@PathVariable("nickname") String nickname){
        return userService.getByNickname(nickname);
    }

    @PatchMapping("update")
    public UserProfileDTO updateProfile(@RequestBody @Valid UserProfileDTO userProfileDTO){
        return userService.updateProfile(userProfileDTO);
    }

    @DeleteMapping("/hard-delete/{id}")
    public ResponseEntity<String> hardDelete(@PathVariable("id") Long id){
        if(userService.hardDelete(id)){
            return new ResponseEntity<>("삭제", HttpStatus.OK);
        }else{
            return new ResponseEntity<>("삭제 실패", HttpStatus.OK);
        }
    }

//    @DeleteMapping("/soft-delete")
//    public String softDelete(){
//
//    };
}