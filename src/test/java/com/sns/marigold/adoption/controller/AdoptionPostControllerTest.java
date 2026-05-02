package com.sns.marigold.adoption.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sns.marigold.adoption.exception.AdoptionPostException;
import com.sns.marigold.adoption.service.AdoptionPostService;
import com.sns.marigold.global.UrlConstants;
import com.sns.marigold.global.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdoptionPostControllerTest {

  @Mock private AdoptionPostService adoptionPostService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new AdoptionPostController(adoptionPostService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("삭제된 입양 게시글 상세 조회 시 410에러를 반환한다")
  void getAdoptionPostDetail_DeletedPost_ReturnsGone() throws Exception {
    Long postId = 100L;
    given(adoptionPostService.getDetail(postId))
        .willThrow(AdoptionPostException.forAdoptionPostDeleted());

    mockMvc
        .perform(get(UrlConstants.ADOPTION_BASE + "/{id}", postId))
        .andExpect(status().isGone())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.status").value(410))
        .andExpect(jsonPath("$.errorCode").value("ADOPTION_POST_DELETED"))
        .andExpect(jsonPath("$.message").value("삭제된 게시글입니다."));
  }
}
