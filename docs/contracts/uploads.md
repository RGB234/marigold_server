# 파일 업로드

게시글·댓글·프로필·채팅 첨부 API는 multipart를 받지 않습니다. 파일은 다음 순서로 보냅니다.

1. `POST /storage/uploads`에 `purpose`(`ADOPTION_POST_IMAGE`, `ADOPTION_COMMENT_IMAGE`, `USER_PROFILE_IMAGE`,
   `CHAT_ATTACHMENT`)와 파일별 `fileName`, `size`를 보냅니다. 응답은 요청 순서대로 `uploadUrl`, `contentType`, `uploadToken`입니다.
2. 각 파일을 `uploadUrl`로 `PUT`합니다. `Content-Type` 헤더에는 응답의 `contentType`을 그대로 넣습니다. 운영에서는 S3 presigned
   URL이라 신고한 크기나 타입과 다르면 S3가 거부하고, 우리 서버의 인증 헤더나 쿠키를 붙이면 안 됩니다.
3. 도메인 API의 JSON 본문에 토큰을 담아 보냅니다. 게시글·댓글·채팅은 `uploadTokens` 목록, 프로필은 `imageUploadToken`입니다.

업로드 URL은 기본 15분, 토큰은 기본 1시간 동안 유효합니다. 토큰이 위조·만료되었거나 다른 사용자·용도의 것이면 `UPLOAD_TOKEN_INVALID`, 파일이 없거나
크기가 다르거나 확정 도중 파일이 바뀌면 `UPLOAD_INCOMPLETE`로 `400`을 반환합니다. 둘 다 파일을 다시 발급받아 올려야 합니다. 토큰은 한 행에만 연결할 수 있고, 이미 다른
행에 연결한 토큰은 `409 UPLOAD_ALREADY_USED`입니다. 저장에 실패한 요청은 토큰이 유효한 동안 같은 토큰으로 다시 보낼 수 있습니다. 서버가 이미
확정한 파일은 다시 검사하거나 옮기지 않으므로, 확정 뒤 같은 업로드 URL로 다시 올린 파일은 무시됩니다. `/storage/files/**`는 로컬 프로필에서만
활성화되며, 운영 프로필은 S3 URL을 사용합니다.

기본 유효시간의 기준은 [StorageUploadProperties](../../src/main/java/com/sns/marigold/storage/config/StorageUploadProperties.java)입니다. 서버 쪽 파일 상태와 정리 과정은 [파일 저장](../storage.md)을 참고합니다.
