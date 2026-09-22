package com.sns.marigold.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.sns.marigold.global.tsid.TsidType;

import io.hypersistence.tsid.TSID;

class WebConfigTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
    new WebConfig().addFormatters(conversionService);

    mockMvc =
        MockMvcBuilders.standaloneSetup(new FormatterController())
            .setConversionService(conversionService)
            .build();
  }

  @Test
  void convertsPlainLongAsDecimal() throws Exception {
    mockMvc
        .perform(get("/formatter/plain/{value}", "1700000000"))
        .andExpect(status().isOk())
        .andExpect(content().string("1700000000"));
  }

  @Test
  void convertsOnlyAnnotatedLongAsTsid() throws Exception {
    long value = 991000000000000001L;

    mockMvc
        .perform(get("/formatter/tsid/{value}", TSID.from(value).toString()))
        .andExpect(status().isOk())
        .andExpect(content().string(Long.toString(value)));
  }

  @RestController
  private static class FormatterController {

    @GetMapping("/formatter/plain/{value}")
    String plain(@PathVariable("value") Long value) {
      return value.toString();
    }

    @GetMapping("/formatter/tsid/{value}")
    String tsid(@PathVariable("value") @TsidType Long value) {
      return value.toString();
    }
  }
}
