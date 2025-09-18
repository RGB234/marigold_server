package com.sns.marigold.adoption.entity;

import com.sns.marigold.global.enums.Species;
import com.sns.marigold.global.enums.Sex;
import com.sns.marigold.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AdoptionInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "writer_id", nullable = false)
  private User writer;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime modifiedAt;

  private Species species;

  private String name;

  private int age;

  private Sex sex;

  private String location;

  private Double weight;

  private boolean neutering;

  @Lob
  private String features;

  @Builder
  public AdoptionInfo(User writer, Species species, String name,
    int age, Sex sex, String location,
    Double weight, boolean neutering, String features) {
    this.writer = writer;
    this.species = species;
    this.name = name;
    this.age = age;
    this.sex = sex;
    this.location = location;
    this.weight = weight;
    this.neutering = neutering;
    this.features = features;
  }
}
