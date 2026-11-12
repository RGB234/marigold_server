package com.sns.marigold.auth.event;

import java.util.List;

/** 로그아웃, 재사용 감지, 탈퇴로 인증 세션이 폐기됨. 그 세션으로 맺은 실시간 연결을 끊는 데 씁니다. */
public record AuthSessionsRevokedEvent(List<String> sessionIds) {}
