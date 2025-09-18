package com.sns.marigold.user.repository;

import com.sns.marigold.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
