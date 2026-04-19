package com.sns.marigold.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthStatus;
import com.sns.marigold.auth.common.enums.Role;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.user.entity.User;
import com.sns.marigold.user.repository.UserRepository;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class ApiIntegrationTest extends BaseIntegrationTest {

  @Autowired protected MockMvc mockMvc;

  @Autowired protected ObjectMapper objectMapper;

  @Autowired protected JwtManager jwtManager;

  @Autowired protected UserRepository userRepository;

  protected User tester1, tester2;

  @BeforeEach
  void apiSetUp() {
    tester1 =
        userRepository.save(User.builder().nickname("tester1").role(Role.ROLE_PERSON).build());

    tester2 =
        userRepository.save(User.builder().nickname("tester2").role(Role.ROLE_PERSON).build());
  }

  protected String getAccessToken(User tester) {
    CustomPrincipal principal =
        new CustomPrincipal(
            tester.getId(),
            Collections.singletonList(new SimpleGrantedAuthority(tester.getRole().name())),
            Map.of(),
            AuthStatus.LOGIN_SUCCESS);

    return jwtManager.createAccessToken(principal);
  }
}
