package com.sns.marigold.adoption.specification;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;

import org.springframework.data.jpa.domain.Specification;

public class AdoptionInfoSpecification {

  /**
   * 종(Species) 필터링
   * @param species 종 (null이면 필터링하지 않음 - 전체 검색)
   * @return Specification
   */
  public static Specification<AdoptionInfo> hasSpecies(Species species) {
    return (root, query, builder) -> {
      if (species == null) {
        return null; // 조건 없음 = 전체 검색
      }
      return builder.equal(root.get("species"), species);
    };
  }

  /**
   * 성별(Sex) 필터링
   * @param sex 성별 (null이면 필터링하지 않음 - 전체 검색)
   * @return Specification
   */
  public static Specification<AdoptionInfo> hasSex(Sex sex) {
    return (root, query, builder) -> {
      if (sex == null) {
        return null; // 조건 없음 = 전체 검색
      }
      return builder.equal(root.get("sex"), sex);
    };
  }

  public static Specification<AdoptionInfo> hasNickname(String nickname) {
    return (root, query, builder) -> {
      if (nickname == null) {
        return null; // 조건 없음 = 전체 검색
      }
      return builder.equal(root.get("nickname"), nickname);
    };
  }
  // public static Specification<AdoptionInfo> hasWriterId(UUID writerId){
  //   return (root, query, builder) -> writerId == null ? null : builder.equal(root.get("writerId"), writerId);
  // }
}