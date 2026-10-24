package com.sns.marigold.user.event;

/**
 * 회원 탈퇴 시 다른 도메인이 자신의 데이터를 정리하도록 알립니다.
 *
 * <p>리스너는 동기 {@code @EventListener}로 탈퇴 트랜잭션 안에서 실행되어야 합니다. 리스너가 실패하면 탈퇴 전체가 롤백됩니다.
 */
public record UserDeletedEvent(Long userId) {}
