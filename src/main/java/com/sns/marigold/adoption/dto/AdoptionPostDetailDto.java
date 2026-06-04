package com.sns.marigold.adoption.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.sns.marigold.adoption.entity.AdoptionPost;
import com.sns.marigold.adoption.entity.AdoptionPostImage;
import com.sns.marigold.adoption.enums.AdoptionPostStatus;
import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;
import com.sns.marigold.user.dto.response.UserInfoDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "입양 게시글 상세 응답")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionPostDetailDto {

  @Schema(description = "입양 게시글 ID", example = "1")
  private Long id;

  @Schema(description = "작성자 정보")
  private UserInfoDto writer;

  @Schema(description = "생성 시각", example = "2026-06-04T12:34:56")
  private LocalDateTime createdAt;

  @Schema(description = "수정 시각", example = "2026-06-04T13:34:56", nullable = true)
  private LocalDateTime modifiedAt;

  @Schema(description = "동물 종", example = "DOG")
  private Species species;

  @Schema(description = "게시글 제목", example = "가족을 찾습니다")
  private String title;

  @Schema(description = "나이", example = "3")
  private Integer age;

  @Schema(description = "성별", example = "MALE")
  private Sex sex;

  @Schema(description = "지역", example = "서울")
  private String area;

  @Schema(description = "몸무게(kg)", example = "4.5")
  private Double weight;

  @Schema(description = "중성화 여부", example = "YES")
  private Neutering neutering;

  @Schema(description = "특징 설명", example = "사람을 좋아하고 산책을 좋아합니다.")
  private String features;

  @Schema(description = "스토리지에 저장된 이미지 파일명 목록")
  private List<String> imageFileNames; // 원본 파일명

  @Schema(description = "다운로드 가능한 이미지 URL 목록")
  @Setter
  private List<String> imageUrls; // 다운로드 가능한 URL

  @Schema(description = "게시글 상태", example = "OPEN")
  private AdoptionPostStatus status;

  @Schema(description = "선택된 입양자 정보", nullable = true)
  @Setter
  private UserInfoDto adopter;

  @Schema(description = "연결된 채팅방 수", example = "3")
  @Setter
  private Integer chatRoomCount;

  @Schema(description = "삭제 여부", example = "false")
  private boolean isDeleted;

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
        .isDeleted(adoptionPost.getDeletedAt() != null)
        .build();
  }
}
