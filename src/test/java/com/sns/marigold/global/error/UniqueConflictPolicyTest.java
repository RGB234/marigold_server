package com.sns.marigold.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;

import com.sns.marigold.user.exception.UserError;

class UniqueConflictPolicyTest {
  @Test
  void duplicateKeyUsesDomainCodeAndHttp409WithoutSqlDetails() {
    var exception =
        new DataIntegrityViolationException(
            "private SQL",
            new ConstraintViolationException(
                "private value",
                new SQLException("private value", "23000", 1062),
                "users.uk_users_nickname"));
    assertThat(UniqueConflictPolicy.resolve(exception))
        .isEqualTo(UserError.NICKNAME_ALREADY_EXISTS);
    var response =
        new GlobalExceptionHandler(null, null)
            .handleDataIntegrityViolation(exception, new MockHttpServletRequest());
    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody().getProperties())
        .containsEntry("errorCode", "USER_NICKNAME_ALREADY_EXISTS");
    assertThat(response.getBody().getDetail())
        .isEqualTo(UserError.NICKNAME_ALREADY_EXISTS.publicMessage());
  }

  @Test
  void unknownUniqueNameStillReturnsConflict() {
    assertThat(UniqueConflictPolicy.resolve(new SQLException("duplicate", "23000", 1062)))
        .isEqualTo(CommonError.RESOURCE_CONFLICT);
  }

  @Test
  void foreignKeyAndNotNullViolationsAreNotConflicts() {
    assertThat(UniqueConflictPolicy.resolve(new SQLException("foreign key", "23000", 1452)))
        .isNull();
    assertThat(UniqueConflictPolicy.resolve(new SQLException("not null", "23000", 1048))).isNull();
  }
}
