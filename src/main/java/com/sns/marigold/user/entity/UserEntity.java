package com.sns.marigold.user.entity;

import com.sns.marigold.global.type.GenderType;
import com.sns.marigold.user.dto.UserSignUpDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@Table(name = "user")
@Getter
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

    @Column(length = 20, nullable = false)
    private String nickname;

    //    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
//    private LocalDate birthday;
    private String birthday;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private GenderType gender;
//    private String gender;

    //    @Column(length=100, nullable = false)
    @Column(length = 100, nullable = true)
    private String photoURL;

    public UserEntity() {
    }

    public static UserEntityBuilder builder() {
        return new UserEntityBuilder();
    }

    public UserEntity toUserEntity(UserSignUpDTO userSignUpDTO) {
        return UserEntity.builder().email(userSignUpDTO.getEmail())
            .password(userSignUpDTO.getPassword())
            .nickname(userSignUpDTO.getNickname()).birthday(userSignUpDTO.getBirthday())
            .gender(userSignUpDTO.getGender())
            .photoURL(userSignUpDTO.getPhotoURL())
            .build();
    }

    public static class UserEntityBuilder {

        private Long id;
        private String email;
        private String password;
        private String nickname;
        private String birthday;
        private GenderType gender;
        private String photoURL;

        UserEntityBuilder() {
        }

        public UserEntityBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserEntityBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserEntityBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserEntityBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public UserEntityBuilder birthday(String birthday) {
            this.birthday = birthday;
            return this;
        }

        public UserEntityBuilder gender(GenderType gender) {
            this.gender = gender;
            return this;
        }

        public UserEntityBuilder photoURL(String photoURL) {
            this.photoURL = photoURL;
            return this;
        }

        public UserEntity build() {
            return new UserEntity(this.id, this.email, this.password, this.nickname, this.birthday,
                this.gender, this.photoURL);
        }

        public String toString() {
            return "UserEntity.UserEntityBuilder(id=" + this.id + ", email=" + this.email
                + ", password=" + this.password + ", nickname=" + this.nickname + ", birthday="
                + this.birthday + ", gender=" + this.gender + ", photoURL=" + this.photoURL + ")";
        }
    }
}


