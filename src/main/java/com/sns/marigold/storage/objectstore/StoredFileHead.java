package com.sns.marigold.storage.objectstore;

/**
 * 저장된 파일의 전체 크기와 형식 감지용 앞부분 바이트.
 *
 * @param size 전체 크기(byte)
 * @param head 앞부분 바이트. 파일이 요청 길이보다 짧으면 파일 전체.
 * @param version 읽은 파일의 버전(S3 ETag). {@link ObjectStorage#promoteUpload}에 넘겨 검사한 파일만 옮기게 한다. 바꿔치기가
 *     불가능한 저장소는 null.
 */
public record StoredFileHead(long size, byte[] head, String version) {}
