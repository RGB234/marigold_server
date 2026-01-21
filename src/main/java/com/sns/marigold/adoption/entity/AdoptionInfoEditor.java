package com.sns.marigold.adoption.entity;

import com.sns.marigold.adoption.enums.Neutering;
import com.sns.marigold.adoption.enums.Sex;
import com.sns.marigold.adoption.enums.Species;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdoptionInfoEditor {
    private final String title;
    private final Integer age;
    private final Double weight;
    private final String features;
    private final String area;
    private final Species species;
    private final Sex sex;
    private final Neutering neutering;    
}
