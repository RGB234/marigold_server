package com.sns.marigold.global.error.exception;

/**
 * 이미 존재하는 사용자가 있을 때 발생하는 예외
 */
public class UserAlreadyExistsException extends RuntimeException {

  public UserAlreadyExistsException() {
    super("이미 존재하는 사용자입니다.");
  }

  public UserAlreadyExistsException(String message) {
    super(message);
  }

  public UserAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
