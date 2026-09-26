package com.sns.marigold.global.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ImageCountPolicyTest {

  @Test
  void allowsCountsWithinRange() {
    ImageCountPolicy policy = new ImageCountPolicy(1, 8);

    assertThat(policy.allows(1)).isTrue();
    assertThat(policy.allows(8)).isTrue();
    assertThat(policy.allows(0)).isFalse();
    assertThat(policy.allows(9)).isFalse();
  }

  @Test
  void rejectsInvalidRange() {
    assertThatThrownBy(() -> new ImageCountPolicy(-1, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ImageCountPolicy(2, 1))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
