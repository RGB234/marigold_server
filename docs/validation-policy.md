# Validation Policy

백엔드 Java 상수 `ValidationPolicy`가 입력값 검증 정책의 기준이다.
프론트엔드는 `validation-policy.json` 사본을 사용하고, `npm run check:validation-policy`로 백엔드 사본과 동기화 여부를 확인한다.

정책 변경 순서는 Java 상수 → `back/src/main/resources/validation-policy.json` → `front/src/global/validation/validation-policy.json`이다. 백엔드에서 `./gradlew test --tests '*ValidationPolicyContractTest'`, 프론트엔드에서 `npm run check:validation-policy`를 실행한다. 프론트 검사만으로는 Java 상수와 JSON의 일치 여부를 검증할 수 없다.

| 영역 | 정책 |
| --- | --- |
| 이메일 | 공백 불가, 이메일 형식 |
| 비밀번호 | 8자 이상 |
| 닉네임 | 한글, 영문, 숫자만 허용. 2자 이상 12자 이하 |
| 입양글 제목 | 빈 문자열 또는 공백만 있는 값 불가. 16자 이하 |
| 입양글 나이 | 0 이상 |
| 입양글 무게 | 0 이상 |
| 입양글 특징 | 빈 문자열 또는 공백만 있는 값 불가. 20자 이상 500자 이하 |
| 입양글 이미지 | 1개 이상 8개 이하 |
| 댓글 내용 | 빈 문자열 또는 공백만 있는 값 불가. 1000자 이하 |
| 댓글 이미지 | 0개 이상 1개 이하 |
| 이미지 파일 | JPG, JPEG, PNG, WebP. 5MiB 이하 |
| 채팅 첨부 | 최대 6개. 파일 1개 5MiB 이하. 총 30MiB 이하 |
| 채팅 첨부 확장자 | csv, docx, hwpx, jpeg, jpg, pdf, png, pptx, txt, webp, xlsx |
| TSID | Crockford Base32 13자 |

1MiB는 1,048,576바이트다. Spring 설정의 `5MB`, `50MB`는 각각 5MiB, 50MiB에 해당한다. 서블릿 수신 한도는 파일당 5MiB, 요청당 50MiB이며, 요청 한도는 게시글 이미지 최대 40MiB와 multipart 부가 데이터를 수용하기 위한 값이다. 각 API의 파일 개수·총용량 정책은 별도로 검증한다.

서블릿 수신 한도를 초과하면 HTTP 413과 `FILE_TOO_LARGE`를 반환한다. 배포 프록시의 요청 한도도 맞춰야 하며, 프록시에서 먼저 거부한 응답은 애플리케이션의 오류 형식을 따르지 않을 수 있다.

Tomcat의 `max-swallow-size`는 64MiB다. 거부된 업로드의 남은 본문을 이 범위까지 읽어 클라이언트가 오류 응답을 받도록 한다. 남은 본문이 이 한도를 넘으면 연결이 종료될 수 있다. [Tomcat 설정 설명](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html)
