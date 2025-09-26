package com.sns.marigold.auth.oauth2;

import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class RandomUsernameGenerator {

  private final int rand_length;

  private static final String[] adjectives = {
    "노래하는", "기다리는", "일하는", "춤추는", "서성이는", "사랑하는", "아름다운", "노는", "인내하는", "방랑하는"
  }; // 10
  private static final String[] nouns = {
    "쥐", "소", "호랑이", "토끼", "용", "뱀", "말", "양", "원숭이", "닭", "개", "돼지"
  }; // 12

  private final Random random;

  public RandomUsernameGenerator() {
    this.rand_length = 4;
    random = new Random();
  }

  public String generate() {
    String adjective = adjectives[random.nextInt(adjectives.length)];
    String noun = nouns[random.nextInt(nouns.length)];
    String randNumber = Integer.toString(random.nextInt((int) Math.pow(10, rand_length)));
    return adjective + noun + randNumber;
  }
}
