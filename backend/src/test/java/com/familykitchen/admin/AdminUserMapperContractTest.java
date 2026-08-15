package com.familykitchen.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.familykitchen.admin.mapper.AdminUserMapper;
import java.lang.reflect.Method;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

/**
 * 验证平台管理用户MapperContract相关业务契约与回归场景。
 */
class AdminUserMapperContractTest {
  @Test
  void userDirectoryExcludesSafelyDeletedAccounts() throws Exception {
    Method method = AdminUserMapper.class.getMethod("list", String.class);
    String sql = String.join(" ", method.getAnnotation(Select.class).value());

    assertThat(sql).contains("u.status<>'CANCELLED'");
  }
}
