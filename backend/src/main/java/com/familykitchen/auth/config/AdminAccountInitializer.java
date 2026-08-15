package com.familykitchen.auth.config;

import com.familykitchen.auth.security.PasswordCodec;
import com.familykitchen.user.mapper.UserMapper;
import com.familykitchen.user.mapper.UserRoleMapper;
import com.familykitchen.user.model.entity.UserDO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 初始化统一用户模型中的平台管理员账号。 */
@Component
public class AdminAccountInitializer {
  private final UserMapper userMapper;
  private final UserRoleMapper userRoleMapper;
  private final PasswordCodec passwordCodec;
  private final AuthBootstrapProperties properties;

  /**
   * 创建平台管理AccountInitializer实例。
   *
   * @param userMapper 用户Mapper
   * @param userRoleMapper 用户角色Mapper
   * @param passwordCodec 密码Codec
   * @param properties properties
   */
  public AdminAccountInitializer(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                 PasswordCodec passwordCodec, AuthBootstrapProperties properties) {
    this.userMapper = userMapper;
    this.userRoleMapper = userRoleMapper;
    this.passwordCodec = passwordCodec;
    this.properties = properties;
  }

  /**
   * 处理平台管理AccountInitializer。
   */
  @PostConstruct
  @Transactional
  public void initialize() {
    String username = required(properties.username(), "family-kitchen.auth.bootstrap-admin.username").toLowerCase();
    UserDO user = userMapper.findByUsername(username);
    if (user == null) {
      user = new UserDO();
      user.setUsername(username);
      user.setPasswordHash(passwordCodec.encode(required(properties.password(), "family-kitchen.auth.bootstrap-admin.password")));
      user.setPasswordAlgorithm("BCRYPT");
      user.setCredentialStatus("ACTIVE");
      user.setNickname(required(properties.displayName(), "family-kitchen.auth.bootstrap-admin.display-name"));
      user.setStatus("ACTIVE");
      userMapper.insert(user);
    }
    if (userRoleMapper.countRole(user.getId(), "PLATFORM_ADMIN") == 0) {
      userRoleMapper.insert(user.getId(), "PLATFORM_ADMIN");
    }
  }

  private static String required(String source, String name) {
    if (source == null || source.isBlank()) {
      throw new IllegalStateException("缺少必需的 application.yml 配置：" + name);
    }
    return source.trim();
  }
}
