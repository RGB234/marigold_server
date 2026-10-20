package com.sns.marigold.global.error.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  @Test
  void assignsTheSameRequestIdToRequestAndResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    MockHttpServletResponse response = new MockHttpServletResponse();

    new RequestIdFilter().doFilter(request, response, new MockFilterChain());

    assertThat(RequestIdFilter.find(request)).isNotBlank();
    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(RequestIdFilter.find(request));
  }
}
