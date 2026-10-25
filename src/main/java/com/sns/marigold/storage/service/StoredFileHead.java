package com.sns.marigold.storage.service;

/**
 * 저장된 파일의 전체 크기와 형식 감지용 앞부분 바이트.
 *
 * @param size 전체 크기(byte)
 * @param head 앞부분 바이트. 파일이 요청 길이보다 짧으면 파일 전체.
 */
public record StoredFileHead(long size, byte[] head) {}
