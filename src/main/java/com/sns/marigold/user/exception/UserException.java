package com.sns.marigold.user.exception;

import com.sns.marigold.global.error.ErrorCode;
import com.sns.marigold.global.error.exception.BusinessException;
import org.springframework.lang.NonNull;

public class UserException extends BusinessException {
  protected UserException(@NonNull ErrorCode errorCode) {
    super(errorCode);
  }

  public static UserException forUserNotFound() {
    return new UserException(ErrorCode.USER_NOT_FOUND);
  }

  public static UserException forUserAlreadyExists() {
    return new UserException(ErrorCode.USER_ALREADY_EXISTS);
  }

  public static UserException forUserNicknameAlreadyExists() {
    return new UserException(ErrorCode.USER_NICKNAME_ALREADY_EXISTS);
  }

  public static UserException forUserLocalCredentialsAlreadyExists() {
    return new UserException(ErrorCode.USER_LOCAL_CREDENTIALS_ALREADY_EXISTS);
  }

  public static UserException forUserOAuth2AlreadyLinked() {
    return new UserException(ErrorCode.USER_OAUTH2_ALREADY_LINKED);
  }

  public static UserException forUserOAuth2AccountAlreadyInUse() {
    return new UserException(ErrorCode.USER_OAUTH2_ACCOUNT_ALREADY_IN_USE);
  }

  public static UserException forUserDeleted() {
    return new UserException(ErrorCode.USER_DELETED);
  }

  public static UserException forUserBanned() {
    return new UserException(ErrorCode.USER_BANNED);
  }

  public static UserException forUserSleeping() {
    return new UserException(ErrorCode.USER_SLEEPING);
  }
}
