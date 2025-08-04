package com.sns.marigold.user.entity;

import com.sns.marigold.global.type.GenderType;
import com.sns.marigold.user.dto.UserProfileDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 20, unique = true, nullable = false)
    private String nickname;

    //    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
//    private LocalDate birthday;
    private String birthday;

    @Column(length = 10, nullable = false)
    private GenderType gender;
//    private String gender;

    //    @Column(length=100, nullable = false)
    @Column(length = 100, nullable = true)
    private String photoURL;

    public UserEntity() {
    }

    public UserProfileDTO toUserProfileDTO(){
        return UserProfileDTO.builder()
            .nickname(nickname)
            .birthday(birthday)
            .gender(gender)
            .photoURL(photoURL)
            .build();
    }
}


