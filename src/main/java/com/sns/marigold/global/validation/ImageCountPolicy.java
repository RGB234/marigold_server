package com.sns.marigold.global.validation;

public record ImageCountPolicy(int min, int max) {

  public ImageCountPolicy {
    if (min < 0 || max < min) {
      throw new IllegalArgumentException("Invalid image count range");
    }
  }

  public boolean allows(int count) {
    return count >= min && count <= max;
  }
}
