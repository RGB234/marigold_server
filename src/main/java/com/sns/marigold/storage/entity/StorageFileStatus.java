package com.sns.marigold.storage.entity;

public enum StorageFileStatus {
  /** URL을 발급했고 아직 도메인 행에 연결되지 않았다. 만료되면 고아 정리가 지운다. */
  PENDING,
  /** 도메인 행이 참조한다. */
  ATTACHED,
  /** 교체·삭제되어 저장소에서 지워야 한다. 행은 이력으로 남긴다. */
  DELETED
}
