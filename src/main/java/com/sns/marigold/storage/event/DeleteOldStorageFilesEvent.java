package com.sns.marigold.storage.event;

import java.util.List;

// 성공 시: 기존 파일 삭제 (List<String> 형태의 파일명 목록을 받음)
public record DeleteOldStorageFilesEvent(List<String> fileNames) {}