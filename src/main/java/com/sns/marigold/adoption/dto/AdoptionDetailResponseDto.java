package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionImage;
import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.enums.AdoptionStatus;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.user.dto.response.UserInfoDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionDetailResponseDto {

  private Long id;

  private UserInfoDto writer;

  private LocalDateTime createdAt;

  private LocalDateTime modifiedAt;

  private Species species;

  private String title;

  private Integer age;

  private Sex sex;

  private String area;

  private Double weight;

  private Neutering neutering;

  private String features;

  private List<String> imageFileNames; // 원본 파일명

  @Setter
  private List<String> imageUrls; // 다운로드 가능한 URL

  private AdoptionStatus status;

  public static AdoptionDetailResponseDto from(AdoptionInfo adoptionInfo) {

    List<String> imageFileNames = adoptionInfo.getImages().stream()
      .map(AdoptionImage::getStoredFileName)
      .collect(Collectors.toList());

    UserInfoDto writer = UserInfoDto.from(adoptionInfo.getWriter());

    return AdoptionDetailResponseDto.builder()
      .id(adoptionInfo.getId())
      .writer(writer)
      .createdAt(adoptionInfo.getCreatedAt())
      .modifiedAt(adoptionInfo.getModifiedAt())
      .species(adoptionInfo.getSpecies())
      .title(adoptionInfo.getTitle())
      .age(adoptionInfo.getAge())
      .sex(adoptionInfo.getSex())
      .area(adoptionInfo.getArea())
      .weight(adoptionInfo.getWeight())
      .neutering(adoptionInfo.getNeutering())
      .features(adoptionInfo.getFeatures())
      .imageFileNames(imageFileNames)
      .status(adoptionInfo.getStatus())
      .build();
  }
}
