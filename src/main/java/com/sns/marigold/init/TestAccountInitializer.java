package com.sns.marigold.init;

import com.sns.marigold.user.dto.InstitutionUserCreateDto;
import com.sns.marigold.user.service.InstitutionUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class TestAccountInitializer implements CommandLineRunner {

  @Autowired
  private InstitutionUserService institutionUserService;

  @Override
  public void run(String... args) throws Exception {
    // 매번 실행 시 가계정 생성
    InstitutionUserCreateDto dto = InstitutionUserCreateDto
      .builder()
      .email("a@a")
      .password("!qwer1234")
      .companyName("함뷰기")
      .repName("함츄쵸")
      .brn("111-22-3333")
      .zipCode("11111")
      .address("해씨별")
      .detailedAddress("해씨동")
      .build();

    institutionUserService.create(dto);
  }
}

