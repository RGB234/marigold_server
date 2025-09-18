package com.sns.marigold.volunteering.entity;

import com.sns.marigold.user.entity.InstitutionUser;
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

@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class VolunteeringRecruitment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreatedDate
  private LocalDateTime createdAt;

  @LastModifiedDate
  private LocalDateTime modifiedAt;

  private String location;

  private LocalDateTime date;

  @Lob
  private String text;

  @ManyToOne(fetch = FetchType.LAZY, optional = false) // not null
  @JoinColumn(name = "writer_id", nullable = false)
  private InstitutionUser writer;

  @Builder
  public VolunteeringRecruitment(String location, LocalDateTime date, String text,
    InstitutionUser writer) {
    this.location = location;
    this.date = date;
    this.text = text;
    this.writer = writer;
  }
}
