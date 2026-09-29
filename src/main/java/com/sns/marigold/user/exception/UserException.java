package com.sns.marigold.user.exception;

import org.springframework.lang.NonNull;

import com.sns.marigold.global.error.exception.ApplicationException;

public class UserException extends ApplicationException {
  protected UserException(@NonNull UserError error) {
    super(error);
  }

  public static UserException forUserNotFound() {
    return new UserException(UserError.NOT_FOUND);
  }

  public static UserException forUserAlreadyExists() {
    return new UserException(UserError.ALREADY_EXISTS);
  }

  public static UserException forUserNicknameAlreadyExists() {
    return new UserException(UserError.NICKNAME_ALREADY_EXISTS);
  }

  public static UserException forUserLocalCredentialsAlreadyExists() {
    return new UserException(UserError.LOCAL_CREDENTIALS_ALREADY_EXISTS);
  }

  public static UserException forUserOAuth2AlreadyLinked() {
    return new UserException(UserError.OAUTH2_ALREADY_LINKED);
  }

  public static UserException forUserOAuth2AccountAlreadyInUse() {
    return new UserException(UserError.OAUTH2_ACCOUNT_ALREADY_IN_USE);
  }

  public static UserException forUserDeleted() {
    return new UserException(UserError.DELETED);
  }

  public static UserException forUserBanned() {
    return new UserException(UserError.BANNED);
  }

  public static UserException forUserSleeping() {
    return new UserException(UserError.SLEEPING);
  }
}
