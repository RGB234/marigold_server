package com.sns.marigold.area.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity
public class Sido {

  @Id
  private long id;

  private String name;

  private String code;

  private LocalDateTime version;
}
