package com.sns.marigold.adoption.specification;

import com.sns.marigold.adoption.entity.AdoptionInfo;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.global.enums.Species;
import org.springframework.data.jpa.domain.Specification;

public class AdoptionInfoSpecification {

  public static Specification<AdoptionInfo> hasSpecies(Species species) {
    return (root, query, builder) -> species == null ? null
      : builder.equal(root.get("species"), species);
  }

  public static Specification<AdoptionInfo> hasSex(Sex sex) {
    return (root, query, builder) -> sex == null ? null : builder.equal(root.get("sex"), sex);
  }
}
