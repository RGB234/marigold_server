# Validation Policy

백엔드 Java 상수 `ValidationPolicy`가 입력값 검증 정책의 기준이다.
프론트엔드는 `validation-policy.json` 사본을 사용하고, `npm run check:validation-policy`로 백엔드 사본과 동기화 여부를 확인한다.

| 영역 | 정책 |
| --- | --- |
| 이메일 | 공백 불가, 이메일 형식 |
| 비밀번호 | 8자 이상 |
| 닉네임 | 한글, 영문, 숫자만 허용. 2자 이상 12자 이하 |
| 입양글 제목 | 공백 불가. 16자 이하 |
| 입양글 나이 | 0 이상 |
| 입양글 무게 | 0 이상 |
| 입양글 특징 | 공백 불가. 20자 이상 500자 이하 |
| 입양글 이미지 | 1개 이상 8개 이하 |
| 댓글 내용 | 공백 불가. 1000자 이하 |
| 댓글 이미지 | 0개 이상 1개 이하 |
| 이미지 파일 | JPG, JPEG, PNG, WebP. 5MB 이하 |
| 채팅 첨부 | 최대 6개. 파일 1개 5MB 이하. 총 30MB 이하 |
| 채팅 첨부 확장자 | csv, docx, hwpx, jpeg, jpg, pdf, png, pptx, txt, webp, xlsx |
| TSID | Crockford Base32 13자 |
