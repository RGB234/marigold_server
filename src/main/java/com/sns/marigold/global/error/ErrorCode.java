package com.sns.marigold.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

  // Common
  INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
  BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),

  // Storage
  FILE_INVALID(HttpStatus.BAD_REQUEST, "FILE_INVALID", "파일이 올바르지 않습니다."),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE_NOT_FOUND", "파일을 찾을 수 없습니다."),
  FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다."),

  // Auth
  AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "인증이 필요합니다."),
  AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH_ACCESS_DENIED", "권한이 없습니다."),
  AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_INVALID", "토큰이 유효하지 않습니다."),
  AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "토큰이 만료되었습니다."),
  AUTH_INVALID_PROVIDER(
      HttpStatus.BAD_REQUEST, "AUTH_INVALID_PROVIDER", "지원하지 않는 OAuth2 Provider입니다."),
  AUTH_INTERNAL_SERVER_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_INTERNAL_SERVER_ERROR", "인증과정에서 서버 오류가 발생했습니다."),

  AUTH_OAUTH2_LOGIN_FAILURE(
      HttpStatus.BAD_REQUEST, "AUTH_OAUTH2_LOGIN_FAILURE", "OAuth2 로그인이 실패했습니다."),
  AUTH_OAUTH2_SIGNUP_FAILURE(
      HttpStatus.BAD_REQUEST, "AUTH_OAUTH2_SIGNUP_FAILURE", "OAuth2 회원가입이 실패했습니다."),
  AUTH_OAUTH2_USER_INFO_NOT_FOUND(
      HttpStatus.BAD_REQUEST, "AUTH_OAUTH2_USER_INFO_NOT_FOUND", "OAuth2 사용자 정보를 찾을 수 없습니다."),

  // #### Entity ####
  ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", "존재하지 않는 엔티티입니다."),
  ENTITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "ENTITY_ALREADY_EXISTS", "이미 존재하는 엔티티입니다."),
  // User
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 사용자입니다."),
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다."),
  USER_NICKNAME_ALREADY_EXISTS(
      HttpStatus.CONFLICT, "USER_NICKNAME_ALREADY_EXISTS", "이미 존재하는 닉네임입니다."),
  // AdoptionPost
  ADOPTION_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "ADOPTION_POST_NOT_FOUND", "존재하지 않는 입양 게시글입니다."),
  ADOPTION_POST_COMPLETED(
      HttpStatus.BAD_REQUEST, "ADOPTION_POST_COMPLETED", "입양 완료된 게시글은 수정할 수 없습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;
}
