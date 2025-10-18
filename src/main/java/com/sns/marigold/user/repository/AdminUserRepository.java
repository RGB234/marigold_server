package com.sns.marigold.user.repository;

import com.sns.marigold.user.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

}
