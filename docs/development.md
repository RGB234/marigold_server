# 개발과 검증

실행 준비는 [설정 안내](configuration.md), 코드 구조는 [아키텍처](architecture.md)를 봅니다.

```powershell
.\gradlew.bat test
.\gradlew.bat spotlessCheck
```

DB 연동 테스트는 Docker 등 해당 테스트의 실행 조건을 준비합니다.

## 변경 종류별 관리

- API 계약 변경: 컨트롤러·DTO의 OpenAPI 애노테이션을 수정하고, 공통 규칙이 바뀌면 [API 가이드](api-guide.md)에 반영합니다.
- 인증·WebSocket 계약 변경: [인증 흐름](auth-flow.md)을 수정하고 프론트엔드 영향을 검토합니다.
- Entity 및 관계 변경: [ERD](erd.md)에 반영합니다.
- 검증 정책 변경: [검증 정책](validation-policy.md)의 순서로 백엔드와 프론트엔드를 함께 수정·검증합니다.
- 설정·환경변수 변경: YAML, `.env.example`, [설정 안내](configuration.md)를 함께 검토합니다.
- 구조·책임 변경: [아키텍처](architecture.md)와 [아키텍처 다이어그램](backend-architecture.mmd)을 함께 수정합니다.

## PR 규칙

[백엔드 PR 템플릿](../.github/pull_request_template.md)을 사용해 변경 내용과 실행한 검증·결과를 기록합니다. 관련 문서와 환경변수 예제를 함께 반영하고, 해당하지 않으면 사유를 적습니다. 프론트엔드와 연계된 변경은 해당 저장소의 PR 또는 기준 커밋을 연결합니다.
