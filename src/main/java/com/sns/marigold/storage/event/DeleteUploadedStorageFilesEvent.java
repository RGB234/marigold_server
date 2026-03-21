package com.sns.marigold.storage.event;

import java.util.List;

// 실패 시: 방금 올린 새 파일 롤백(삭제)
public record DeleteUploadedStorageFilesEvent(List<String> fileNames) {}
