package com.sns.marigold.user;

import com.sns.marigold.user.dto.UserSignUpDTO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller("/user")
public class UserController {
    @PostMapping("/create")
    public String create(@RequestBody UserSignUpDTO userSignUpDTO){

        return userSignUpDTO.toString();
    }

    @GetMapping("/get/{id}")
    public String get(@PathVariable("id") String id){
        return id;
    }

    @PatchMapping("update/{id}")
    public String update(@PathVariable("id") String id){
        return id;
    }

    @DeleteMapping("/delete")
    public String delete(){
        return "deleted";
    }
}