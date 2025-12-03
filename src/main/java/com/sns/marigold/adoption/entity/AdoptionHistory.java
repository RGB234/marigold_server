package com.sns.marigold.adoption.entity;
import com.sns.marigold.user.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdoptionHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto increment
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adoption_info_id", nullable = false)
  private AdoptionInfo adoptionInfo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "adopter_id", nullable = false)
  private User adopter;

  private LocalDateTime date;
}
