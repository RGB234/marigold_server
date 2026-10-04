package com.sns.marigold.global.error;

import java.sql.SQLException;
import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;

import com.sns.marigold.adoption.exception.AdoptionError;
import com.sns.marigold.chat.exception.ChatError;
import com.sns.marigold.user.exception.UserError;

/** MySQL duplicate key 오류만 충돌로 분류합니다. FK/NOT NULL 오류는 서버 오류로 유지합니다. */
final class UniqueConflictPolicy {
  private static final Map<String, ErrorSpec> ERRORS =
      Map.of(
          "uk_users_email", UserError.ALREADY_EXISTS,
          "uk_users_nickname", UserError.NICKNAME_ALREADY_EXISTS,
          "uk_users_provider", UserError.OAUTH2_ACCOUNT_ALREADY_IN_USE,
          "uk_adoption_adopter_post", AdoptionError.POST_ALREADY_COMPLETED,
          "uk_user_image_user", UserError.IMAGE_CONFLICT,
          "uk_room_participant_room_user", ChatError.PARTICIPANT_ALREADY_EXISTS);

  private UniqueConflictPolicy() {}

  static ErrorSpec resolve(Throwable exception) {
    String constraint = null;
    boolean duplicate = false;
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        constraint = violation.getConstraintName();
      }
      if (cause instanceof SQLException sql && sql.getErrorCode() == 1062) {
        duplicate = true;
      }
    }
    if (!duplicate) {
      return null;
    }
    if (constraint == null) {
      return CommonError.RESOURCE_CONFLICT;
    }
    String name = constraint.replace("`", "").replace("'", "");
    name = name.substring(name.lastIndexOf('.') + 1);
    return ERRORS.getOrDefault(name, CommonError.RESOURCE_CONFLICT);
  }
}
