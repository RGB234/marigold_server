package com.sns.marigold.auth.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.dto.AuthPrincipalDto;
import com.sns.marigold.auth.common.dto.LoginResponseDto;
import com.sns.marigold.global.tsid.TsidCodec;

class AuthSessionContractTest {

  @Test
  void serializesThePrincipalIdAsTsidString() throws Exception {
    LoginResponseDto response =
        new LoginResponseDto("access-token", new AuthPrincipalDto(1L, List.of("ROLE_PERSON")));

    String json = new ObjectMapper().writeValueAsString(response);

    assertThat(json).contains("\"accessToken\":\"access-token\"");
    assertThat(json).contains("\"userId\":\"" + TsidCodec.encode(1L) + "\"");
    assertThat(json).contains("\"authorities\":[\"ROLE_PERSON\"]");
  }
}
