package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.admin.model.dto.AdminUserCreateRequest;
import com.familykitchen.auth.model.dto.RegisterRequest;
import com.familykitchen.user.model.dto.ChangeUsernameRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** Ensures every account-name entry uses the same two-character minimum. */
class UsernameLengthValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsTwoCharacterAccountNames() {
    assertTrue(validator.validate(new RegisterRequest("ab", "123456", "用户", null)).isEmpty());
    assertTrue(validator.validate(new AdminUserCreateRequest(
        "ab", "123456", "用户", null, false)).isEmpty());
    assertTrue(validator.validate(new ChangeUsernameRequest("ab")).isEmpty());
  }

  @Test
  void rejectsOneCharacterAccountNames() {
    assertFalse(validator.validate(new RegisterRequest("a", "123456", "用户", null)).isEmpty());
    assertFalse(validator.validate(new AdminUserCreateRequest(
        "a", "123456", "用户", null, false)).isEmpty());
    assertFalse(validator.validate(new ChangeUsernameRequest("a")).isEmpty());
  }
}
