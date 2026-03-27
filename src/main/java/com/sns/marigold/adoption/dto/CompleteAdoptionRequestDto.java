package com.sns.marigold.adoption.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sns.marigold.global.util.TsidJacksonConfig;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CompleteAdoptionRequestDto {
  @JsonDeserialize(using = TsidJacksonConfig.Deserializer.class)
  private Long adopterId;
}
