package com.sns.marigold.global.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderType {
    MALE("MALE", "남성"),
    FEMALE("FEMALE", "여성"),
    OTHER("OTHER", "기타");

    private final String gender;
    @JsonValue // JAVA -> JSON
    private final String description;

    @JsonCreator // JSON -> JAVA
    public static GenderType fromGenderType(String genderType) {
        return Arrays.stream(values()).filter(g -> g.gender.equals(genderType)).findAny()
            .orElse(null);
    }
}

