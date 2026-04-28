package com.sns.marigold.global.config;

import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DummyDataLoader implements ApplicationRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("Checking for dummy data...");

    List<String> dummyEmails =
        List.of("user1@example.com", "user2@example.com", "user3@example.com");

    String defaultPassword = "!password123";

    for (int i = 0; i < dummyEmails.size(); i++) {
      String email = dummyEmails.get(i);
      String nickname = "테스트유저" + (i + 1);

      if (!userRepository.existsByEmail(email)) {
        User user =
            User.builder()
                .email(email)
                .password(passwordEncoder.encode(defaultPassword))
                .nickname(nickname)
                .build();

        userRepository.save(user);
        log.info("Created dummy user: {} / {}", email, defaultPassword);
      }
    }
  }
}
