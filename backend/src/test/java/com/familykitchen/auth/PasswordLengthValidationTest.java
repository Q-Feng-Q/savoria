package com.familykitchen.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.familykitchen.admin.model.dto.AdminUserCreateRequest;
import com.familykitchen.auth.model.dto.PasswordResetRequest;
import com.familykitchen.auth.model.dto.RegisterRequest;
import com.familykitchen.user.model.dto.ChangePasswordRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** 验证所有设置新密码的入口统一采用 6 至 64 位长度规则。 */
class PasswordLengthValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsSixCharacterPasswordAtEveryPasswordCreationEntry() {
    assertTrue(validator.validate(new RegisterRequest("tester", "123456", "测试用户", null)).isEmpty());
    assertTrue(validator.validate(new ChangePasswordRequest("old-password", "123456")).isEmpty());
    assertTrue(validator.validate(new PasswordResetRequest("user@example.com", "123456", "123456")).isEmpty());
    assertTrue(validator.validate(new AdminUserCreateRequest(
        "tester", "123456", "测试用户", null, false)).isEmpty());
  }

  @Test
  void rejectsPasswordShorterThanSixCharactersAtEveryPasswordCreationEntry() {
    assertFalse(validator.validate(new RegisterRequest("tester", "12345", "测试用户", null)).isEmpty());
    assertFalse(validator.validate(new ChangePasswordRequest("old-password", "12345")).isEmpty());
    assertFalse(validator.validate(new PasswordResetRequest("user@example.com", "123456", "12345")).isEmpty());
    assertFalse(validator.validate(new AdminUserCreateRequest(
        "tester", "12345", "测试用户", null, false)).isEmpty());
  }
}
