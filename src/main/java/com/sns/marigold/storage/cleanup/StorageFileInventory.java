package com.sns.marigold.storage.cleanup;

import java.util.List;
import java.util.function.Consumer;

public interface StorageFileInventory {

  // 참조 조회의 IN 목록 길이도 이 값을 넘지 않는다
  int PAGE_SIZE = 1000;

  /** 저장된 파일을 {@link #PAGE_SIZE}개 이하의 비어 있지 않은 페이지로 나눠 넘긴다. 전체 목록을 메모리에 올리지 않는다. */
  void forEachPage(Consumer<List<StoredFile>> pageConsumer);
}
