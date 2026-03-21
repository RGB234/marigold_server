package com.sns.marigold.adoption.dto;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.entity.AdoptionPostImage;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
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
public class AdoptionPostDetailDto {

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

  @Setter private List<String> imageUrls; // 다운로드 가능한 URL

  private AdoptionPostStatus status;

  public static AdoptionPostDetailDto from(AdoptionPost adoptionPost) {

    List<String> imageFileNames =
        adoptionPost.getImages().stream()
            .map(AdoptionPostImage::getStoredFileName)
            .collect(Collectors.toList());

    UserInfoDto writer = UserInfoDto.from(adoptionPost.getWriter());

    return AdoptionPostDetailDto.builder()
        .id(adoptionPost.getId())
        .writer(writer)
        .createdAt(adoptionPost.getCreatedAt())
        .modifiedAt(adoptionPost.getModifiedAt())
        .species(adoptionPost.getSpecies())
        .title(adoptionPost.getTitle())
        .age(adoptionPost.getAge())
        .sex(adoptionPost.getSex())
        .area(adoptionPost.getArea())
        .weight(adoptionPost.getWeight())
        .neutering(adoptionPost.getNeutering())
        .features(adoptionPost.getFeatures())
        .imageFileNames(imageFileNames)
        .status(adoptionPost.getStatus())
        .build();
  }
}
